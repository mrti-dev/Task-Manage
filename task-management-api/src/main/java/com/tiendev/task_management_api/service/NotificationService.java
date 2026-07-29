package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.NotificationResponse;
import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.model.Notification;
import com.tiendev.task_management_api.model.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PageResponse<NotificationResponse> getByReceiverId(Long receiverId, Pageable pageable);
    PageResponse<NotificationResponse> getUnreadByReceiverId(Long receiverId, Pageable pageable);
    long countUnread(Long receiverId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long receiverId);
    Notification createNotification(String title, String content, NotificationType type,
                                    Long receiverId, Long senderId, Long taskId, Long projectId, Long workspaceId);
}
