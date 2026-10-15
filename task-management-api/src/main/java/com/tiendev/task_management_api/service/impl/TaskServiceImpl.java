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
	private final com.tiendev.task_management_api.repository.TagRepository tagRepository;
	private final com.tiendev.task_management_api.service.TaskDependencyService taskDependencyService;

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

		if (request.getParentId() != null) {
			Task parent = taskRepository.findById(request.getParentId()).orElseThrow(
					() -> new ResourceNotFoundException("Parent task not found with id: " + request.getParentId()));
			if (parent.getParentTask() != null) {
				throw new InvalidOperationException("Subtasks cannot be nested more than 1 level");
			}
			task.setParentTask(parent);
		}

		if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
			task.setTags(new java.util.HashSet<>(tagRepository.findAllById(request.getTagIds())));
		}

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
		List<TaskResponse> content = mapTaskListToResponses(page.getContent());
		return PageResponse.from(page, content);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<TaskResponse> getAll(TaskFilterRequest filter, Pageable pageable) {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		Specification<Task> spec = Specification
				.where(TaskSpecifications.isActive())
				.and(TaskSpecifications.inWorkspaces(memberWorkspaceIds))
				.and(TaskSpecifications.fromFilter(filter))
				.and(TaskSpecifications.fetchRelations());
		Page<Task> page = taskRepository.findAll(spec, pageable);
		List<TaskResponse> content = mapTaskListToResponses(page.getContent());
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
		boolean isOwner = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, getCurrentUserId(), WorkspaceRole.OWNER);

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
			if (request.getStatus() == TaskStatus.DONE && !oldStatus.equals(TaskStatus.DONE)) {
				taskDependencyService.validateCanTransitionToDone(task.getId());
			}
			task.setStatus(request.getStatus());
		}
		if (request.getTagIds() != null) {
			task.setTags(new java.util.HashSet<>(tagRepository.findAllById(request.getTagIds())));
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
		if (request.isClearDeadline() && request.getDeadline() != null) {
			throw new BusinessException("Cannot provide both deadline and clearDeadline flag");
		}
		if (request.isClearDeadline()) {
			task.setDeadline(null);
		} else if (request.getDeadline() != null) {
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
		boolean deadlineChanged = (request.isClearDeadline() && oldDeadline != null)
				|| (request.getDeadline() != null && !request.getDeadline().equals(oldDeadline));

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

		if (deadlineChanged && assigneeUser != null) {
			notificationService.createNotification(
					"Hạn chót nhiệm vụ đã thay đổi: " + task.getTitle(),
					"Hạn chót của nhiệm vụ \"" + task.getTitle() + "\" đã được thay đổi thành "
							+ (task.getDeadline() != null ? task.getDeadline() : "không có hạn") + ".",
					NotificationType.TASK_DEADLINE_CHANGED,
					assigneeUser.getId(), currentUserId, task.getId(),
					task.getProject().getId(), workspaceId);
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
					request.isClearDeadline() ? null : request.getDeadline().toString(),
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
		boolean isOwner = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, currentUserId, WorkspaceRole.OWNER);
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
		Set<Long> ownerIds = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
				.stream().map(m -> m.getUser().getId()).collect(Collectors.toSet());
		List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
		for (WorkspaceMember member : members) {
			Long memberUserId = member.getUser().getId();
			if (memberUserId.equals(senderId) || memberUserId.equals(skipUserId)) {
				if (!ownerIds.contains(memberUserId)) {
					continue;
				}
			}
			notificationService.createNotification(
					title, content, type,
					memberUserId, senderId,
					taskId, projectId, workspaceId);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.List<TaskResponse> getCalendarTasks(Long workspaceId, Long projectId, Long assigneeId,
			TaskPriority priority, TaskStatus status, LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			throw new InvalidOperationException("Both 'from' and 'to' dates are required");
		}
		if (from.isAfter(to)) {
			throw new InvalidOperationException("'from' date must be before or equal to 'to' date");
		}
		long days = java.time.temporal.ChronoUnit.DAYS.between(from, to);
		if (days > 366) {
			throw new InvalidOperationException("Date range cannot exceed 366 days");
		}

		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		if (memberWorkspaceIds.isEmpty()) {
			return java.util.Collections.emptyList();
		}

		java.util.Collection<Long> queryWorkspaceIds;
		if (workspaceId != null) {
			if (!memberWorkspaceIds.contains(workspaceId)) {
				throw new InvalidOperationException("You do not have access to this workspace");
			}
			queryWorkspaceIds = java.util.Collections.singleton(workspaceId);
		} else {
			queryWorkspaceIds = memberWorkspaceIds;
		}

		List<Task> tasks = taskRepository.findCalendarTasks(
				queryWorkspaceIds, from, to, projectId, assigneeId, priority, status);

		return tasks.stream().map(this::toTaskResponse).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public java.util.List<TaskResponse> getSubtasks(Long taskId) {
		Task parent = taskRepository.findById(taskId)
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
		validateMemberInWorkspace(getCurrentUserId(), parent.getProject().getWorkspace().getId());
		return parent.getSubtasks().stream()
				.filter(Task::isActive)
				.map(this::toTaskResponse)
				.toList();
	}

	private java.util.List<TaskResponse> mapTaskListToResponses(java.util.List<Task> tasks) {
		if (tasks.isEmpty()) {
			return java.util.Collections.emptyList();
		}
		java.util.List<Long> taskIds = tasks.stream().map(Task::getId).toList();
		java.util.Map<Long, int[]> subtaskCounts = new java.util.HashMap<>();
		for (Object[] row : taskRepository.countSubtasksByParentTaskIds(taskIds)) {
			Long parentId = (Long) row[0];
			int total = ((Number) row[1]).intValue();
			int completed = row[2] != null ? ((Number) row[2]).intValue() : 0;
			subtaskCounts.put(parentId, new int[]{total, completed});
		}

		java.util.Map<Long, java.util.List<com.tiendev.task_management_api.dto.TagResponse>> tags = new java.util.HashMap<>();
		for (Object[] row : taskRepository.findTagRowsByTaskIds(taskIds)) {
			tags.computeIfAbsent((Long) row[0], ignored -> new java.util.ArrayList<>())
					.add(com.tiendev.task_management_api.dto.TagResponse.builder()
							.id((Long) row[1]).workspaceId((Long) row[2]).name((String) row[3])
							.color((String) row[4]).build());
		}

		java.util.Map<Long, java.util.List<com.tiendev.task_management_api.dto.ChecklistItemResponse>> checklists = new java.util.HashMap<>();
		for (Object[] row : taskRepository.findChecklistRowsByTaskIds(taskIds)) {
			checklists.computeIfAbsent((Long) row[0], ignored -> new java.util.ArrayList<>())
					.add(com.tiendev.task_management_api.dto.ChecklistItemResponse.builder()
							.id((Long) row[1]).taskId((Long) row[0]).title((String) row[2])
							.completed((Boolean) row[3]).position(((Number) row[4]).intValue())
							.createdAt((java.time.LocalDateTime) row[5]).build());
		}

		java.util.Map<Long, Integer> commentCounts = taskRepository.countCommentsByTaskIds(taskIds).stream()
				.collect(java.util.stream.Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).intValue()));

		return tasks.stream()
				.map(task -> toTaskListResponse(task, subtaskCounts, tags, checklists, commentCounts))
				.toList();
	}

	private TaskResponse toTaskListResponse(Task task, java.util.Map<Long, int[]> subtaskCounts,
			java.util.Map<Long, java.util.List<com.tiendev.task_management_api.dto.TagResponse>> tags,
			java.util.Map<Long, java.util.List<com.tiendev.task_management_api.dto.ChecklistItemResponse>> checklists,
			java.util.Map<Long, Integer> commentCounts) {
		UserResponse assigneeResponse = null;
		if (task.getAssignee() != null) {
			assigneeResponse = UserResponse.builder().id(task.getAssignee().getId())
					.username(task.getAssignee().getUsername()).email(task.getAssignee().getEmail())
					.avatar(task.getAssignee().getAvatar()).role(task.getAssignee().getRole()).build();
		}

		LocalDate today = LocalDate.now();
		boolean overdue = task.getDeadline() != null && task.getDeadline().isBefore(today) && task.getStatus() != TaskStatus.DONE;
		boolean dueSoon = task.getDeadline() != null && !task.getDeadline().isBefore(today) && !task.getDeadline().isAfter(today.plusDays(2)) && task.getStatus() != TaskStatus.DONE;

		Long parentTaskId = task.getParentTask() != null ? task.getParentTask().getId() : null;
		int[] counts = subtaskCounts != null ? subtaskCounts.getOrDefault(task.getId(), new int[]{0, 0}) : new int[]{0, 0};

		return TaskResponse.builder().id(task.getId()).workspaceId(task.getProject().getWorkspace().getId())
				.projectId(task.getProject().getId()).projectName(task.getProject().getName()).title(task.getTitle())
				.description(task.getDescription()).priority(task.getPriority()).status(task.getStatus())
				.assignee(assigneeResponse).deadline(task.getDeadline())
				.overdue(overdue).dueSoon(dueSoon)
				.parentTaskId(parentTaskId).subtaskCount(counts[0]).completedSubtaskCount(counts[1])
				.tags(tags.getOrDefault(task.getId(), java.util.Collections.emptyList()))
				.checklist(checklists.getOrDefault(task.getId(), java.util.Collections.emptyList()))
				.commentCount(commentCounts.getOrDefault(task.getId(), 0))
				.createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt()).build();
	}

	private TaskResponse toTaskResponse(Task task) {
		UserResponse assigneeResponse = null;
		if (task.getAssignee() != null) {
			assigneeResponse = UserResponse.builder().id(task.getAssignee().getId())
					.username(task.getAssignee().getUsername()).email(task.getAssignee().getEmail())
					.avatar(task.getAssignee().getAvatar()).role(task.getAssignee().getRole()).build();
		}

		LocalDate today = LocalDate.now();
		boolean overdue = task.getDeadline() != null && task.getDeadline().isBefore(today) && task.getStatus() != TaskStatus.DONE;
		boolean dueSoon = task.getDeadline() != null && !task.getDeadline().isBefore(today) && !task.getDeadline().isAfter(today.plusDays(2)) && task.getStatus() != TaskStatus.DONE;

		Long parentTaskId = task.getParentTask() != null ? task.getParentTask().getId() : null;
		int subtaskCount = task.getSubtasks() != null ? task.getSubtasks().size() : 0;
		int completedSubtaskCount = task.getSubtasks() != null ? (int) task.getSubtasks().stream().filter(s -> s.getStatus() == TaskStatus.DONE).count() : 0;

		java.util.List<com.tiendev.task_management_api.dto.TagResponse> tagList = task.getTags() != null ? task.getTags().stream()
				.map(t -> com.tiendev.task_management_api.dto.TagResponse.builder().id(t.getId()).workspaceId(t.getWorkspace().getId()).name(t.getName()).color(t.getColor()).build())
				.toList() : java.util.Collections.emptyList();

		java.util.List<com.tiendev.task_management_api.dto.ChecklistItemResponse> checklistList = task.getChecklistItems() != null ? task.getChecklistItems().stream()
				.sorted(java.util.Comparator.comparingInt(com.tiendev.task_management_api.model.TaskChecklistItem::getPosition))
				.map(c -> com.tiendev.task_management_api.dto.ChecklistItemResponse.builder().id(c.getId()).taskId(task.getId()).title(c.getTitle()).completed(c.isCompleted()).position(c.getPosition()).createdAt(c.getCreatedAt()).build())
				.toList() : java.util.Collections.emptyList();

		return TaskResponse.builder().id(task.getId()).workspaceId(task.getProject().getWorkspace().getId())
				.projectId(task.getProject().getId()).projectName(task.getProject().getName()).title(task.getTitle())
				.description(task.getDescription()).priority(task.getPriority()).status(task.getStatus())
				.assignee(assigneeResponse).deadline(task.getDeadline())
				.overdue(overdue).dueSoon(dueSoon)
				.parentTaskId(parentTaskId).subtaskCount(subtaskCount).completedSubtaskCount(completedSubtaskCount)
				.tags(tagList).checklist(checklistList)
				.commentCount(task.getComments() != null ? task.getComments().size() : 0)
				.createdAt(task.getCreatedAt()).updatedAt(task.getUpdatedAt()).build();
	}
}
