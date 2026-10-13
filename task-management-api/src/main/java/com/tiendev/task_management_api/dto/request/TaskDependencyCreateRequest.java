package com.tiendev.task_management_api.dto.request;

import com.tiendev.task_management_api.model.enums.DependencyType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskDependencyCreateRequest {

    @NotNull(message = "Prerequisite task ID is required")
    private Long prerequisiteTaskId;

    @Builder.Default
    private DependencyType type = DependencyType.DEPENDS_ON;
}
