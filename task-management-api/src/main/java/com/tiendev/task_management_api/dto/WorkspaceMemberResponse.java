package com.tiendev.task_management_api.dto;

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
public class WorkspaceMemberResponse {
    private Long id;
    private Long workspaceId;
    private UserResponse user;
    private WorkspaceRole role;
    private LocalDateTime joinedAt;
}
