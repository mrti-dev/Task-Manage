package com.tiendev.task_management_api.event;

import com.tiendev.task_management_api.service.MailOutbox;
import com.tiendev.task_management_api.service.NotificationService;
import com.tiendev.task_management_api.model.enums.NotificationType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class WorkspaceMemberAddedEventListener {

    private final MailOutbox mailOutbox;
    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handle(WorkspaceMemberAddedEvent event) {
        notificationService.createNotification(
                "Bạn đã được thêm vào workspace: " + event.workspaceName(),
                "Bạn đã được thêm vào workspace \"" + event.workspaceName() + "\" với vai trò " + event.role() + ".",
                NotificationType.WORKSPACE_MEMBER_ADDED,
                event.recipientId(), event.senderId(), null, null, event.workspaceId());
        mailOutbox.enqueue(event);
    }
}
