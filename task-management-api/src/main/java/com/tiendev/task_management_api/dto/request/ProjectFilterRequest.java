package com.tiendev.task_management_api.dto.request;

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
public class ProjectFilterRequest {
    private Long workspaceId;
    private String name;
    private ProjectStatus status;
    private LocalDate startDateFrom;
    private LocalDate startDateTo;
    private LocalDate endDateFrom;
    private LocalDate endDateTo;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
}
