package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.request.TaskCreateRequest;
import com.tiendev.task_management_api.dto.request.TaskFilterRequest;
import com.tiendev.task_management_api.dto.request.TaskUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskResponse>> create(@Valid @RequestBody TaskCreateRequest request) {
        TaskResponse response = taskService.create(request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TaskResponse>>> getAll(
            @ModelAttribute TaskFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<TaskResponse> responses = taskService.getAll(filter, pageable);
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> getById(@PathVariable Long id) {
        TaskResponse response = taskService.getById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TaskResponse>> update(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        TaskResponse response = taskService.update(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ApiResponse.success(null, "Task deleted successfully");
    }
}
