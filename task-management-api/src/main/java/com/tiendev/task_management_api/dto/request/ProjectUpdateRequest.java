package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.ProjectStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectUpdateRequest {
    private String name;
    private String description;
    private ProjectStatus status;
    private LocalDate endDate;
}
