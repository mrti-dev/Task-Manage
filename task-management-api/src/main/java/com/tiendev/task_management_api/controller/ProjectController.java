package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.ProjectResponse;
import com.tiendev.task_management_api.dto.request.ProjectCreateRequest;
import com.tiendev.task_management_api.dto.request.ProjectFilterRequest;
import com.tiendev.task_management_api.dto.request.ProjectUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProjectResponse>> create(@Valid @RequestBody ProjectCreateRequest request) {
        ProjectResponse response = projectService.create(request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ProjectResponse>>> getAll(
            @ModelAttribute ProjectFilterRequest filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ProjectResponse> responses = projectService.getAll(filter, pageable);
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> getById(@PathVariable Long id) {
        ProjectResponse response = projectService.getById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProjectResponse>> update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        ProjectResponse response = projectService.update(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ApiResponse.success(null, "Project deleted successfully");
    }
}
