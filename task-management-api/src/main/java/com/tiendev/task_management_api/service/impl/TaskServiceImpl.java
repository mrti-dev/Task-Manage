package com.tiendev.task_management_api.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.TaskCreateRequest;
import com.tiendev.task_management_api.dto.request.TaskFilterRequest;
import com.tiendev.task_management_api.dto.request.TaskUpdateRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.ProjectRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.spec.TaskSpecifications;
import com.tiendev.task_management_api.service.ActivityLogService;
import com.tiendev.task_management_api.service.NotificationService;
import com.tiendev.task_management_api.service.TaskService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

	private final TaskRepository taskRepository;
	private final ProjectRepository projectRepository;
	private final UserRepository userRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final NotificationService notificationService;
	private final ActivityLogService activityLogService;

	@Override
	@Transactional
	public TaskResponse create(TaskCreateRequest request) {
		Project project = projectRepository.findById(request.getProjectId()).orElseThrow(
				() -> new ResourceNotFoundException("Project not found with id: " + request.getProjectId()));
		if (!project.isActive() || !project.getWorkspace().isActive()) {
			throw new ResourceNotFoundException("Project not found with id: " + request.getProjectId());
		}
		validateOwner(project.getWorkspace().getId());

		User assignee = null;
		if (request.getAssigneeEmail() != null && !request.getAssigneeEmail().isBlank()) {
			assignee = userRepository.findByEmail(request.getAssigneeEmail()).orElseThrow(
					() -> new ResourceNotFoundException("User not found with email: " + request.getAssigneeEmail()));
			validateMemberInWorkspace(assignee.getId(), project.getWorkspace().getId());
		}

		Task task = new Task();
		task.setProject(project);
		task.setTitle(request.getTitle());
		task.setDescription(request.getDescription());
		task.setPriority(request.getPriority());
		task.setStatus(request.getStatus() != null ? request.getStatus() : TaskStatus.TODO);
		task.setAssignee(assignee);
		task.setDeadline(request.getDeadline());

		validateDeadline(task);

		task = taskRepository.save(task);

		Long currentUserId = getCurrentUserId();
		Long workspaceId = project.getWorkspace().getId();

		if (assignee != null) {
			notificationService.createNotification("Nhiệm vụ mới: " + task.getTitle(),
					"Bạn được giao nhiệm vụ \"" + task.getTitle() + "\" trong dự án \"" + project.getName() + "\".",
					NotificationType.TASK_CREATED, assignee.getId(), currentUserId,
					task.getId(), project.getId(), workspaceId);
		}

		sendTaskNotificationToMembers(workspaceId, currentUserId, assignee != null ? assignee.getId() : null,
				NotificationType.TASK_CREATED,
				"Nhiệm vụ mới: " + task.getTitle(),
				"Nhiệm vụ \"" + task.getTitle() + "\" vừa được tạo trong dự án \"" + project.getName() + "\".",
				task.getId(), project.getId());

		activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.CREATED,
				null, null, task.getTitle(), currentUserId, workspaceId);

		return toTaskResponse(task);
	}

	@Override
	@Transactional(readOnly = true)
	public TaskResponse getById(Long id) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
		if (!task.isActive() || !task.getProject().isActive() || !task.getProject().getWorkspace().isActive()) {
			throw new ResourceNotFoundException("Task not found with id: " + id);
		}
		validateMembership(task.getProject().getWorkspace().getId());
		return toTaskResponse(task);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TaskResponse> getAll(Pageable pageable) {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		Page<Task> page = taskRepository.findAllActiveByWorkspaceIds(memberWorkspaceIds, pageable);
		List<TaskResponse> content = page.getContent().stream()
				.map(this::toTaskResponse)
				.toList();
		return PageResponse.from(page, content);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TaskResponse> getAll(TaskFilterRequest filter, Pageable pageable) {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		Specification<Task> spec = Specification
				.where(TaskSpecifications.isActive())
				.and(TaskSpecifications.inWorkspaces(memberWorkspaceIds))
				.and(TaskSpecifications.fromFilter(filter));
		Page<Task> page = taskRepository.findAll(spec, pageable);
		List<TaskResponse> content = page.getContent().stream()
				.map(this::toTaskResponse)
				.toList();
		return PageResponse.from(page, content);
	}

	@Override
	@Transactional
	public TaskResponse update(Long id, TaskUpdateRequest request) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
		if (!task.isActive() || !task.getProject().isActive() || !task.getProject().getWorkspace().isActive()) {
			throw new ResourceNotFoundException("Task not found with id: " + id);
		}
		Long workspaceId = task.getProject().getWorkspace().getId();
		validateMembership(workspaceId);
		boolean isOwner = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
				.filter(owner -> owner.getUser().getId().equals(getCurrentUserId()))
				.isPresent();

		String oldTitle = task.getTitle();
		String oldDescription = task.getDescription();
		TaskStatus oldStatus = task.getStatus();
		TaskPriority oldPriority = task.getPriority();
		Long oldAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
		String oldAssigneeName = task.getAssignee() != null ? task.getAssignee().getUsername() : null;
		LocalDate oldDeadline = task.getDeadline();

		if (request.getTitle() != null) {
			if (!isOwner) throw new InvalidOperationException("Only workspace owner can update the title.");
			task.setTitle(request.getTitle());
		}
		if (request.getDescription() != null) {
			task.setDescription(request.getDescription());
		}
		if (request.getPriority() != null) {
			task.setPriority(request.getPriority());
		}
		if (request.getStatus() != null) {
			task.setStatus(request.getStatus());
		}
		if (request.isUnassign()) {
			if (!isOwner) throw new InvalidOperationException("Only workspace owner can unassign a task.");
			task.setAssignee(null);
		} else if (request.getAssigneeId() != null) {
			Long currentUserId = getCurrentUserId();
			boolean isClaim = oldAssigneeId == null && request.getAssigneeId().equals(currentUserId);
			if (!isOwner && !isClaim) {
				throw new InvalidOperationException("Only workspace owner can change the assignee.");
			}
			User assignee = userRepository.findById(request.getAssigneeId()).orElseThrow(
					() -> new ResourceNotFoundException("User not found with id: " + request.getAssigneeId()));
			validateMemberInWorkspace(assignee.getId(), workspaceId);
			task.setAssignee(assignee);
		}
		if (request.getDeadline() != null) {
			task.setDeadline(request.getDeadline());
			validateDeadline(task);
		}

		task = taskRepository.save(task);

		Long currentUserId = getCurrentUserId();
		Long newAssigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
		User assigneeUser = task.getAssignee();
		String newAssigneeName = task.getAssignee() != null ? task.getAssignee().getUsername() : null;

		boolean statusChanged = request.getStatus() != null && !request.getStatus().equals(oldStatus);
		boolean priorityChanged = request.getPriority() != null && !request.getPriority().equals(oldPriority);
		boolean assigneeChanged = newAssigneeId != null && !newAssigneeId.equals(oldAssigneeId);
		boolean taskClaimed = oldAssigneeId == null && newAssigneeId != null;
		boolean taskUnassigned = request.isUnassign() && oldAssigneeId != null;
		boolean titleChanged = request.getTitle() != null && !request.getTitle().equals(oldTitle);
		boolean descriptionChanged = request.getDescription() != null && !request.getDescription().equals(oldDescription);
		boolean deadlineChanged = request.getDeadline() != null && !request.getDeadline().equals(oldDeadline);

		Long skipUserId;

		if (statusChanged) {
			skipUserId = assigneeUser != null ? assigneeUser.getId() : null;
			if (assigneeUser != null) {
				notificationService.createNotification("Trạng thái nhiệm vụ đã thay đổi: " + task.getTitle(),
						"Nhiệm vụ \"" + task.getTitle() + "\" chuyển từ " + oldStatus + " sang " + task.getStatus()
								+ ".",
						NotificationType.TASK_STATUS_CHANGED, assigneeUser.getId(), currentUserId, task.getId(),
						task.getProject().getId(), workspaceId);
			}
			sendTaskNotificationToMembers(workspaceId, currentUserId, skipUserId,
					NotificationType.TASK_STATUS_CHANGED,
					"Trạng thái nhiệm vụ đã thay đổi: " + task.getTitle(),
					"Nhiệm vụ \"" + task.getTitle() + "\" chuyển từ " + oldStatus + " sang " + task.getStatus() + ".",
					task.getId(), task.getProject().getId());
		}

		if (priorityChanged) {
			skipUserId = assigneeUser != null ? assigneeUser.getId() : null;
			if (assigneeUser != null) {
				notificationService.createNotification("Ưu tiên nhiệm vụ đã thay đổi: " + task.getTitle(),
						"Nhiệm vụ \"" + task.getTitle() + "\" chuyển từ " + oldPriority + " sang " + task.getPriority()
								+ ".",
						NotificationType.TASK_PRIORITY_CHANGED, assigneeUser.getId(), currentUserId, task.getId(),
						task.getProject().getId(), workspaceId);
			}
			sendTaskNotificationToMembers(workspaceId, currentUserId, skipUserId,
					NotificationType.TASK_PRIORITY_CHANGED,
					"Ưu tiên nhiệm vụ đã thay đổi: " + task.getTitle(),
					"Nhiệm vụ \"" + task.getTitle() + "\" chuyển từ " + oldPriority + " sang " + task.getPriority()
							+ ".",
					task.getId(), task.getProject().getId());
		}

		if (taskClaimed) {
			notificationService.createNotification("Nhiệm vụ đã được nhận: " + task.getTitle(),
					assigneeUser.getUsername() + " đã nhận nhiệm vụ \"" + task.getTitle() + "\".",
					NotificationType.TASK_CLAIMED, assigneeUser.getId(), currentUserId, task.getId(),
					task.getProject().getId(), workspaceId);
			sendTaskNotificationToMembers(workspaceId, currentUserId, assigneeUser.getId(),
					NotificationType.TASK_CLAIMED,
					"Nhiệm vụ đã được nhận: " + task.getTitle(),
					assigneeUser.getUsername() + " đã nhận nhiệm vụ \"" + task.getTitle() + "\".",
					task.getId(), task.getProject().getId());
		} else if (assigneeChanged) {
			notificationService.createNotification("Bạn được gán nhiệm vụ: " + task.getTitle(),
					"Bạn được gán nhiệm vụ \"" + task.getTitle() + "\" trong dự án \"" + task.getProject().getName()
							+ "\".",
					NotificationType.TASK_ASSIGNED, newAssigneeId, currentUserId, task.getId(),
					task.getProject().getId(), workspaceId);
			sendTaskNotificationToMembers(workspaceId, currentUserId, newAssigneeId,
					NotificationType.TASK_ASSIGNED,
					"Nhiệm vụ đã được gán: " + task.getTitle(),
					"Nhiệm vụ \"" + task.getTitle() + "\" đã được gán cho "
							+ assigneeUser.getUsername() + " trong dự án \"" + task.getProject().getName() + "\".",
					task.getId(), task.getProject().getId());
		}

		if (taskUnassigned) {
			skipUserId = assigneeUser != null ? assigneeUser.getId() : null;
			sendTaskNotificationToMembers(workspaceId, currentUserId, skipUserId,
					NotificationType.TASK_UPDATED,
					"Nhiệm vụ đã được bỏ gán: " + task.getTitle(),
					"Nhiệm vụ \"" + task.getTitle() + "\" đã được bỏ gán trong dự án \""
							+ task.getProject().getName() + "\".",
					task.getId(), task.getProject().getId());
		}

		if (titleChanged || descriptionChanged || deadlineChanged) {
			sendTaskNotificationToMembers(workspaceId, currentUserId, null,
					NotificationType.TASK_UPDATED,
					"Nhiệm vụ đã được cập nhật: " + task.getTitle(),
					"Nhiệm vụ \"" + task.getTitle() + "\" đã được cập nhật trong dự án \""
							+ task.getProject().getName() + "\".",
					task.getId(), task.getProject().getId());
		}

		if (titleChanged) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.UPDATED,
					"title", oldTitle, request.getTitle(), currentUserId, workspaceId);
		}
		if (descriptionChanged) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.UPDATED,
					"description", oldDescription, request.getDescription(), currentUserId, workspaceId);
		}
		if (statusChanged) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.STATUS_CHANGED,
					"status", oldStatus.name(), task.getStatus().name(), currentUserId, workspaceId);
		}
		if (priorityChanged) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.PRIORITY_CHANGED,
					"priority", oldPriority.name(), task.getPriority().name(), currentUserId, workspaceId);
		}
		if (taskClaimed || assigneeChanged) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.ASSIGNED,
					"assignee", oldAssigneeName, newAssigneeName, currentUserId, workspaceId);
		}
		if (taskUnassigned) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.UNASSIGNED,
					"assignee", oldAssigneeName, null, currentUserId, workspaceId);
		}
		if (deadlineChanged) {
			activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.UPDATED,
					"deadline",
					oldDeadline != null ? oldDeadline.toString() : null,
					request.getDeadline() != null ? request.getDeadline().toString() : null,
					currentUserId, workspaceId);
		}

		return toTaskResponse(task);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Task task = taskRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
		if (!task.isActive() || !task.getProject().isActive() || !task.getProject().getWorkspace().isActive()) {
			throw new ResourceNotFoundException("Task not found with id: " + id);
		}
		validateOwner(task.getProject().getWorkspace().getId());

		Long assigneeId = task.getAssignee() != null ? task.getAssignee().getId() : null;
		String taskTitle = task.getTitle();
		String projectName = task.getProject().getName();
		Long projectId = task.getProject().getId();
		Long workspaceId = task.getProject().getWorkspace().getId();

		task.setActive(false);
		taskRepository.save(task);

		Long senderId = getCurrentUserId();

		activityLogService.log(EntityType.TASK, task.getId(), ActivityAction.DELETED,
				null, null, taskTitle, senderId, workspaceId);

		if (assigneeId != null) {
			notificationService.createNotification(
					"Nhiệm vụ đã bị xóa: " + taskTitle,
					"Nhiệm vụ \"" + taskTitle + "\" đã bị xóa khỏi dự án \"" + projectName + "\".",
					NotificationType.TASK_DELETED,
					assigneeId,
					senderId,
					null,
					projectId,
					workspaceId
			);
		}

		sendTaskNotificationToMembers(workspaceId, senderId, assigneeId,
				NotificationType.TASK_DELETED,
				"Nhiệm vụ đã bị xóa: " + taskTitle,
				"Nhiệm vụ \"" + taskTitle + "\" đã bị xóa khỏi dự án \"" + projectName + "\".",
				null, projectId);
	}

	private Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return Long.valueOf(auth.getName());
	}

	private Set<Long> getMemberWorkspaceIds() {
		Long currentUserId = getCurrentUserId();
		return workspaceMemberRepository.findByUserId(currentUserId).stream()
				.map(m -> m.getWorkspace().getId())
				.collect(Collectors.toSet());
	}

	private void validateMembership(Long workspaceId) {
		Long currentUserId = getCurrentUserId();
		if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserId)) {
			throw new ResourceNotFoundException("Task not found");
		}
	}

	private void validateOwner(Long workspaceId) {
		Long currentUserId = getCurrentUserId();
		boolean isOwner = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
				.filter(owner -> owner.getUser().getId().equals(currentUserId))
				.isPresent();
		if (!isOwner) {
			throw new InvalidOperationException("Only workspace owner can perform this action.");
		}
	}

	private void validateDeadline(Task task) {
		if (task.getDeadline() == null) {
			return;
		}
		Project project = task.getProject();

		if (project.getStartDate() != null && task.getDeadline().isBefore(project.getStartDate())) {
			throw new BusinessException(
					"Task deadline cannot be before project start date (" + project.getStartDate() + ").");
		}
		if (project.getEndDate() != null && task.getDeadline().isAfter(project.getEndDate())) {
			throw new BusinessException(
					"Task deadline cannot be after project end date (" + project.getEndDate() + ").");
		}
	}

	private void validateMemberInWorkspace(Long userId, Long workspaceId) {
		if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)) {
			throw new InvalidOperationException(
					"User is not a member of this workspace and cannot be assigned to a task.");
		}
	}

	private void sendTaskNotificationToMembers(Long workspaceId, Long senderId, Long skipUserId,
			NotificationType type, String title, String content, Long taskId, Long projectId) {
		User owner = getWorkspaceOwner(workspaceId);
		Long ownerId = owner != null ? owner.getId() : null;
		List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
		for (WorkspaceMember member : members) {
			Long memberUserId = member.getUser().getId();
			if (memberUserId.equals(senderId) || memberUserId.equals(skipUserId)) {
				if (!memberUserId.equals(ownerId)) {
					continue;
				}
			}
			notificationService.createNotification(
					title, content, type,
					memberUserId, senderId,
					taskId, projectId, workspaceId);
		}
	}

	private User getWorkspaceOwner(Long workspaceId) {
		return workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
				.map(WorkspaceMember::getUser).orElse(null);
	}

	private TaskResponse toTaskResponse(Task task) {
		UserResponse assigneeResponse = null;
		if (task.getAssignee() != null) {
			assigneeResponse = UserResponse.builder().id(task.getAssignee().getId())
					.username(task.getAssignee().getUsername()).email(task.getAssignee().getEmail())
					.avatar(task.getAssignee().getAvatar()).role(task.getAssignee().getRole()).build();
		}

		return TaskResponse.builder().id(task.getId()).workspaceId(task.getProject().getWorkspace().getId())
				.projectId(task.getProject().getId()).projectName(task.getProject().getName()).title(task.getTitle())
				.description(task.getDescription()).priority(task.getPriority()).status(task.getStatus())
				.assignee(assigneeResponse).deadline(task.getDeadline()).commentCount(task.getComments().size())
				.createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt()).build();
	}
}
