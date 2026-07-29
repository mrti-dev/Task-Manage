package com.tiendev.task_management_api.dto;

import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLogResponse {
    private Long id;
    private EntityType entityType;
    private Long entityId;
    private ActivityAction action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private UserResponse performedBy;
    private Long workspaceId;
    private LocalDateTime createdAt;
}
