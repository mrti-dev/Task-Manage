package com.tiendev.task_management_api.dto;

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
public class TaskResponse {
    private Long id;
    private Long workspaceId;
    private Long projectId;
    private String projectName;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private UserResponse assignee;
    private LocalDate deadline;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
