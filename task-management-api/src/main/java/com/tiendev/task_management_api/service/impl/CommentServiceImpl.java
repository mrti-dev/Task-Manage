package com.tiendev.task_management_api.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendev.task_management_api.dto.CommentResponse;
import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.CommentCreateRequest;
import com.tiendev.task_management_api.dto.request.CommentUpdateRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.Comment;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.repository.CommentRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.service.ActivityLogService;
import com.tiendev.task_management_api.service.CommentService;
import com.tiendev.task_management_api.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

	private final CommentRepository commentRepository;
	private final TaskRepository taskRepository;
	private final UserRepository userRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final ActivityLogService activityLogService;
	private final NotificationService notificationService;
	private final SimpMessagingTemplate messagingTemplate;

	@Override
	@Transactional
	public CommentResponse create(CommentCreateRequest request) {
		Task task = taskRepository.findById(request.getTaskId())
				.orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + request.getTaskId()));
		validateParentChain(task);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long currentUserId = Long.valueOf(auth.getName());
		User user = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

		Long workspaceId = task.getProject().getWorkspace().getId();
		if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, user.getId())) {
			throw new InvalidOperationException("User is not a member of this workspace.");
		}

		Comment comment = new Comment();
		comment.setTask(task);
		comment.setUser(user);
		comment.setContent(request.getContent());

		comment = commentRepository.save(comment);

		activityLogService.log(EntityType.COMMENT, comment.getId(), ActivityAction.CREATED,
				null, null, request.getContent(), user.getId(), workspaceId);

		CommentResponse response = toCommentResponse(comment);
		messagingTemplate.convertAndSend(
				"/topic/workspace/" + workspaceId + "/tasks/" + task.getId() + "/comments",
				response
		);

		if (task.getAssignee() != null && !task.getAssignee().getId().equals(currentUserId)) {
			notificationService.createNotification(
					"Bình luận mới: " + task.getTitle(),
					user.getUsername() + " đã bình luận trong nhiệm vụ \"" + task.getTitle() + "\".",
					NotificationType.COMMENT_CREATED, task.getAssignee().getId(), currentUserId,
					task.getId(), task.getProject().getId(), workspaceId);
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public CommentResponse getById(Long id) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
		validateParentChain(comment.getTask());
		validateMembership(comment.getTask().getProject().getWorkspace().getId());
		return toCommentResponse(comment);
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponse> getAll() {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		return commentRepository.findAll().stream()
				.filter(c -> c.getTask().isActive()
						&& c.getTask().getProject().isActive()
						&& c.getTask().getProject().getWorkspace().isActive()
						&& memberWorkspaceIds.contains(c.getTask().getProject().getWorkspace().getId()))
				.map(this::toCommentResponse)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<CommentResponse> getByTaskId(Long taskId) {
		return commentRepository.findByTaskId(taskId).stream()
				.filter(c -> c.getTask().isActive() && c.getTask().getProject().isActive()
						&& c.getTask().getProject().getWorkspace().isActive())
				.map(this::toCommentResponse)
				.toList();
	}

	@Override
	@Transactional
	public CommentResponse update(Long id, CommentUpdateRequest request) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
		validateParentChain(comment.getTask());
		validateCommentAuthor(comment);

		String oldContent = comment.getContent();
		comment.setContent(request.getContent());
		comment = commentRepository.save(comment);

		Long workspaceId = comment.getTask().getProject().getWorkspace().getId();
		activityLogService.log(EntityType.COMMENT, comment.getId(), ActivityAction.UPDATED,
				"content", oldContent, request.getContent(), getCurrentUserId(), workspaceId);

		return toCommentResponse(comment);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Comment comment = commentRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));
		validateParentChain(comment.getTask());
		validateCommentAuthor(comment);
		Long workspaceId = comment.getTask().getProject().getWorkspace().getId();
		Long commentId = comment.getId();
		String content = comment.getContent();
		commentRepository.delete(comment);

		activityLogService.log(EntityType.COMMENT, commentId, ActivityAction.DELETED,
				null, null, content, getCurrentUserId(), workspaceId);
	}

	private void validateCommentAuthor(Comment comment) {
		Long currentUserId = getCurrentUserId();
		if (!comment.getUser().getId().equals(currentUserId)) {
			throw new InvalidOperationException("You can only edit or delete your own comments.");
		}
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
			throw new ResourceNotFoundException("Comment not found");
		}
	}

	private void validateParentChain(Task task) {
		if (!task.isActive() || !task.getProject().isActive() || !task.getProject().getWorkspace().isActive()) {
            throw new BusinessException("Cannot perform operation: the associated task or its parent entities are deleted.");
		}
	}

	private CommentResponse toCommentResponse(Comment comment) {
		UserResponse userResponse = UserResponse.builder().id(comment.getUser().getId())
				.username(comment.getUser().getUsername()).email(comment.getUser().getEmail())
				.avatar(comment.getUser().getAvatar()).role(comment.getUser().getRole()).build();

		return CommentResponse.builder().id(comment.getId()).taskId(comment.getTask().getId()).user(userResponse)
				.content(comment.getContent()).createdAt(comment.getCreatedAt()).build();
	}
}
