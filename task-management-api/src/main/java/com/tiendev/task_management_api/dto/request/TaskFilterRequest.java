package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
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
public class TaskFilterRequest {
    private Long projectId;
    private String title;
    private TaskPriority priority;
    private TaskStatus status;
    private Long assigneeId;
    private LocalDate deadlineFrom;
    private LocalDate deadlineTo;
    private LocalDateTime createdAtFrom;
    private LocalDateTime createdAtTo;
}
