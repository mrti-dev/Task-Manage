package com.tiendev.task_management_api.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceUpdateRequest {
    @Pattern(regexp = ".*\\S.*", message = "Workspace name must not be blank")
    @Size(max = 100, message = "Workspace name must not exceed 100 characters")
    private String name;

    @Size(max = 500, message = "Workspace description must not exceed 500 characters")
    private String description;
}
