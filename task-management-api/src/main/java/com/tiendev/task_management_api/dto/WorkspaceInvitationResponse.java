package com.tiendev.task_management_api.dto;

import com.tiendev.task_management_api.model.enums.WorkspaceInvitationStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceInvitationResponse {

    private Long id;
    private Long workspaceId;
    private String workspaceName;
    private String email;
    private WorkspaceRole role;
    private WorkspaceInvitationStatus status;
    private String invitedByName;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}
