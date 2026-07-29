package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.WorkspaceResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(@Valid @RequestBody WorkspaceCreateRequest request) {
        WorkspaceResponse response = workspaceService.create(request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceResponse>>> getAll() {
        List<WorkspaceResponse> responses = workspaceService.getAll();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> getById(@PathVariable Long id) {
        WorkspaceResponse response = workspaceService.getById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> update(@PathVariable Long id, @Valid @RequestBody WorkspaceUpdateRequest request) {
        WorkspaceResponse response = workspaceService.update(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workspaceService.delete(id);
        return ApiResponse.success(null, "Workspace deleted successfully");
    }
}
