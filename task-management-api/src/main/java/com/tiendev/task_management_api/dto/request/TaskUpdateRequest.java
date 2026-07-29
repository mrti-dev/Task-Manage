package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskUpdateRequest {
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private Long assigneeId;
    private boolean unassign;
    private LocalDate deadline;
}
