package com.tiendev.task_management_api.dto;

import com.tiendev.task_management_api.model.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectResponse {
    private Long id;
    private Long workspaceId;
    private String workspaceName;
    private String name;
    private String description;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private int taskCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
