package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.SavedFilterResponse;
import com.tiendev.task_management_api.dto.request.SavedFilterCreateRequest;
import com.tiendev.task_management_api.dto.request.SavedFilterUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.SavedFilterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/saved-filters")
@RequiredArgsConstructor
public class SavedFilterController {

    private final SavedFilterService savedFilterService;

    @PostMapping
    public ResponseEntity<ApiResponse<SavedFilterResponse>> create(@Valid @RequestBody SavedFilterCreateRequest request) {
        SavedFilterResponse response = savedFilterService.create(request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SavedFilterResponse>>> getAll(@RequestParam(required = false) Long workspaceId) {
        List<SavedFilterResponse> responses = savedFilterService.getAll(workspaceId);
        return ApiResponse.success(responses);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavedFilterResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SavedFilterUpdateRequest request) {
        SavedFilterResponse response = savedFilterService.update(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        savedFilterService.delete(id);
        return ApiResponse.success(null, "Saved filter deleted successfully");
    }
}
