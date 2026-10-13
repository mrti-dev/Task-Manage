package com.tiendev.task_management_api.dto;

import com.tiendev.task_management_api.model.enums.DependencyType;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependencyResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long prerequisiteTaskId;
    private String prerequisiteTaskTitle;
    private TaskStatus prerequisiteTaskStatus;
    private DependencyType type;
    private LocalDateTime createdAt;
}
