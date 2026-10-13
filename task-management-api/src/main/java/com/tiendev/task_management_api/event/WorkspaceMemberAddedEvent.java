package com.tiendev.task_management_api.event;

import com.tiendev.task_management_api.model.enums.WorkspaceRole;

public record WorkspaceMemberAddedEvent(
        Long recipientId,
        Long senderId,
        Long workspaceId,
        String toEmail,
        String inviterName,
        String workspaceName,
        WorkspaceRole role
) {
}
