package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
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
public class TaskUpdateRequest {
    @Pattern(regexp = ".*\\S.*", message = "Task title must not be blank")
    @Size(max = 200, message = "Task title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Task description must not exceed 1000 characters")
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private Long assigneeId;
    private boolean unassign;
    private LocalDate deadline;
    private boolean clearDeadline;
    private java.util.List<Long> tagIds;
}
