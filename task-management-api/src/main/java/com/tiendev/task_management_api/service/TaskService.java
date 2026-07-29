package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.request.TaskCreateRequest;
import com.tiendev.task_management_api.dto.request.TaskFilterRequest;
import com.tiendev.task_management_api.dto.request.TaskUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface TaskService {
    TaskResponse create(TaskCreateRequest request);
    TaskResponse getById(Long id);
    PageResponse<TaskResponse> getAll(Pageable pageable);
    PageResponse<TaskResponse> getAll(TaskFilterRequest filter, Pageable pageable);
    TaskResponse update(Long id, TaskUpdateRequest request);
    void delete(Long id);
}
