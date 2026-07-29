package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.ProjectResponse;
import com.tiendev.task_management_api.dto.request.ProjectCreateRequest;
import com.tiendev.task_management_api.dto.request.ProjectFilterRequest;
import com.tiendev.task_management_api.dto.request.ProjectUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface ProjectService {
    ProjectResponse create(ProjectCreateRequest request);
    ProjectResponse getById(Long id);
    PageResponse<ProjectResponse> getAll(Pageable pageable);
    PageResponse<ProjectResponse> getAll(ProjectFilterRequest filter, Pageable pageable);
    ProjectResponse update(Long id, ProjectUpdateRequest request);
    void delete(Long id);
}
