package com.tiendev.task_management_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedFilterCreateRequest {

    @NotBlank(message = "Filter name is required")
    @Size(max = 100, message = "Filter name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Criteria JSON is required")
    private String criteriaJson;

    private Long workspaceId;
}
