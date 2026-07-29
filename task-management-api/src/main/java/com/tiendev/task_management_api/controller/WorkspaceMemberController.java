package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.WorkspaceMemberResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.WorkspaceMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspace-members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberService workspaceMemberService;

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> create(@Valid @RequestBody WorkspaceMemberCreateRequest request) {
        WorkspaceMemberResponse response = workspaceMemberService.create(request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceMemberResponse>>> getAll() {
        List<WorkspaceMemberResponse> responses = workspaceMemberService.getAll();
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> getById(@PathVariable Long id) {
        WorkspaceMemberResponse response = workspaceMemberService.getById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceMemberResponse>> update(@PathVariable Long id, @Valid @RequestBody WorkspaceMemberUpdateRequest request) {
        WorkspaceMemberResponse response = workspaceMemberService.update(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        workspaceMemberService.delete(id);
        return ApiResponse.success(null, "WorkspaceMember deleted successfully");
    }
}
