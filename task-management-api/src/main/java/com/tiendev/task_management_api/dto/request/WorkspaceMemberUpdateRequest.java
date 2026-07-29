package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceMemberUpdateRequest {
    @NotNull
    private WorkspaceRole role;
}
