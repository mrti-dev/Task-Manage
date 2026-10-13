package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.WorkspaceDashboardResponse;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<WorkspaceDashboardResponse>> getDashboard(
            @PathVariable Long workspaceId,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        WorkspaceDashboardResponse response = dashboardService.getWorkspaceDashboard(workspaceId, projectId, from, to);
        return ApiResponse.success(response);
    }
}
