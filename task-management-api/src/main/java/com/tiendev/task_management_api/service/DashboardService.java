package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.WorkspaceDashboardResponse;

import java.time.LocalDate;

public interface DashboardService {

    WorkspaceDashboardResponse getWorkspaceDashboard(Long workspaceId, Long projectId, LocalDate from, LocalDate to);
}
