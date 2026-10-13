package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.ProjectStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    @Pattern(regexp = ".*\\S.*", message = "Project name must not be blank")
    @Size(max = 100, message = "Project name must not exceed 100 characters")
    private String name;

    @Size(max = 1000, message = "Project description must not exceed 1000 characters")
    private String description;
    private ProjectStatus status;
    private LocalDate endDate;
    private boolean clearEndDate;
}
