package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.ActivityLogResponse;
import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import org.springframework.data.domain.Pageable;

public interface ActivityLogService {
    void log(EntityType entityType, Long entityId, ActivityAction action,
             String fieldName, String oldValue, String newValue, Long performedBy, Long workspaceId);

    PageResponse<ActivityLogResponse> getByEntity(EntityType entityType, Long entityId, Pageable pageable);
    PageResponse<ActivityLogResponse> getByWorkspaceId(Long workspaceId, Pageable pageable);
}
