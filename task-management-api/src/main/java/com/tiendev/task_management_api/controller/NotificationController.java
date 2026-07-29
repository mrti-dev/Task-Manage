package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.NotificationResponse;
import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getByReceiverId(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        checkNotificationOwner(userId);
        PageResponse<NotificationResponse> responses = notificationService.getByReceiverId(userId, pageable);
        return ApiResponse.success(responses);
    }

    @GetMapping("/user/{userId}/unread")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> getUnread(
            @PathVariable Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        checkNotificationOwner(userId);
        PageResponse<NotificationResponse> responses = notificationService.getUnreadByReceiverId(userId, pageable);
        return ApiResponse.success(responses);
    }

    @GetMapping("/user/{userId}/unread-count")
    public ResponseEntity<ApiResponse<Long>> countUnread(@PathVariable Long userId) {
        checkNotificationOwner(userId);
        long count = notificationService.countUnread(userId);
        return ApiResponse.success(count);
    }

    @PutMapping("/user/{userId}/read/{id}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long userId, @PathVariable Long id) {
        checkNotificationOwner(userId);
        notificationService.markAsRead(id, userId);
        return ApiResponse.success(null, "Notification marked as read");
    }

    @PutMapping("/user/{userId}/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(@PathVariable Long userId) {
        checkNotificationOwner(userId);
        notificationService.markAllAsRead(userId);
        return ApiResponse.success(null, "All notifications marked as read");
    }

    private void checkNotificationOwner(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(auth.getName());
        if (!currentUserId.equals(userId)) {
            throw new InvalidOperationException("You can only access your own notifications.");
        }
    }
}
