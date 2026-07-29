package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.NotificationResponse;
import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.*;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.repository.*;
import com.tiendev.task_management_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getByReceiverId(Long receiverId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toNotificationResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getUnreadByReceiverId(Long receiverId, Pageable pageable) {
        Page<Notification> page = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(receiverId, pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toNotificationResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long receiverId) {
        return notificationRepository.countByReceiverIdAndIsReadFalse(receiverId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new InvalidOperationException("You can only mark your own notifications as read.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long receiverId) {
        List<Notification> unread = notificationRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(receiverId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    @Transactional
    public Notification createNotification(String title, String content, NotificationType type,
                                          Long receiverId, Long senderId, Long taskId, Long projectId, Long workspaceId) {
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found with id: " + receiverId));
        User sender = senderId != null
                ? userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found with id: " + senderId))
                : null;
        Task task = taskId != null
                ? taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId))
                : null;
        Project project = projectId != null
                ? projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + projectId))
                : null;
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType(type);
        notification.setReceiver(receiver);
        notification.setSender(sender);
        notification.setTask(task);
        notification.setProject(project);
        notification.setWorkspace(workspace);

        notification = notificationRepository.save(notification);

        NotificationResponse response = toNotificationResponse(notification);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(receiverId),
                "/queue/notifications",
                response
        );

        return notification;
    }

    private String generateLink(Notification notification) {
        if (notification.getTask() != null && notification.getProject() != null) {
            Long workspaceId = notification.getWorkspace().getId();
            Long projectId = notification.getProject().getId();
            Long taskId = notification.getTask().getId();
            return "/workspaces/" + workspaceId + "/projects/" + projectId + "/tasks/" + taskId;
        }
        if (notification.getProject() != null) {
            Long workspaceId = notification.getWorkspace().getId();
            Long projectId = notification.getProject().getId();
            return "/workspaces/" + workspaceId + "/projects/" + projectId;
        }
        return "/workspaces/" + notification.getWorkspace().getId();
    }

    private NotificationResponse toNotificationResponse(Notification notification) {
        UserResponse receiverResponse = UserResponse.builder()
                .id(notification.getReceiver().getId())
                .username(notification.getReceiver().getUsername())
                .email(notification.getReceiver().getEmail())
                .role(notification.getReceiver().getRole())
                .build();

        UserResponse senderResponse = null;
        if (notification.getSender() != null) {
            senderResponse = UserResponse.builder()
                    .id(notification.getSender().getId())
                    .username(notification.getSender().getUsername())
                    .email(notification.getSender().getEmail())
                    .role(notification.getSender().getRole())
                    .build();
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .isRead(notification.isRead())
                .receiver(receiverResponse)
                .sender(senderResponse)
                .taskId(notification.getTask() != null ? notification.getTask().getId() : null)
                .projectId(notification.getProject() != null ? notification.getProject().getId() : null)
                .workspaceId(notification.getWorkspace().getId())
                .link(generateLink(notification))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
