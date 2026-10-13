package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.TaskDependencyResponse;
import com.tiendev.task_management_api.dto.request.TaskDependencyCreateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.TaskDependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/dependencies")
@RequiredArgsConstructor
public class TaskDependencyController {

    private final TaskDependencyService dependencyService;

    @PostMapping
    public ResponseEntity<ApiResponse<TaskDependencyResponse>> addDependency(
            @PathVariable Long taskId,
            @Valid @RequestBody TaskDependencyCreateRequest request) {
        TaskDependencyResponse response = dependencyService.addDependency(taskId, request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TaskDependencyResponse>>> getDependencies(@PathVariable Long taskId) {
        List<TaskDependencyResponse> responses = dependencyService.getDependencies(taskId);
        return ApiResponse.success(responses);
    }

    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<ApiResponse<Void>> deleteDependency(
            @PathVariable Long taskId,
            @PathVariable Long dependencyId) {
        dependencyService.deleteDependency(taskId, dependencyId);
        return ApiResponse.success(null, "Dependency deleted successfully");
    }
}
