package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.ActivityLogResponse;
import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<ApiResponse<PageResponse<ActivityLogResponse>>> getByEntity(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ActivityLogResponse> responses = activityLogService.getByEntity(entityType, entityId, pageable);
        return ApiResponse.success(responses);
    }

    @GetMapping("/workspace/{workspaceId}")
    public ResponseEntity<ApiResponse<PageResponse<ActivityLogResponse>>> getByWorkspace(
            @PathVariable Long workspaceId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ActivityLogResponse> responses = activityLogService.getByWorkspaceId(workspaceId, pageable);
        return ApiResponse.success(responses);
    }
}
