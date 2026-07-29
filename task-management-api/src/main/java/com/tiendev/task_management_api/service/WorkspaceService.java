package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.WorkspaceResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceUpdateRequest;

import java.util.List;

public interface WorkspaceService {
    WorkspaceResponse create(WorkspaceCreateRequest request);
    WorkspaceResponse getById(Long id);
    List<WorkspaceResponse> getAll();
    WorkspaceResponse update(Long id, WorkspaceUpdateRequest request);
    void delete(Long id);
}
