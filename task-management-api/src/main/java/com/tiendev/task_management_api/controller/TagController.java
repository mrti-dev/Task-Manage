package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.TagResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.request.TagCreateRequest;
import com.tiendev.task_management_api.dto.request.TagUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @PostMapping("/api/workspaces/{workspaceId}/tags")
    public ResponseEntity<ApiResponse<TagResponse>> createTag(
            @PathVariable Long workspaceId,
            @Valid @RequestBody TagCreateRequest request) {
        TagResponse response = tagService.createTag(workspaceId, request);
        return ApiResponse.created(response);
    }

    @GetMapping("/api/workspaces/{workspaceId}/tags")
    public ResponseEntity<ApiResponse<List<TagResponse>>> getWorkspaceTags(@PathVariable Long workspaceId) {
        List<TagResponse> responses = tagService.getWorkspaceTags(workspaceId);
        return ApiResponse.success(responses);
    }

    @PutMapping("/api/tags/{tagId}")
    public ResponseEntity<ApiResponse<TagResponse>> updateTag(
            @PathVariable Long tagId,
            @Valid @RequestBody TagUpdateRequest request) {
        TagResponse response = tagService.updateTag(tagId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/api/tags/{tagId}")
    public ResponseEntity<ApiResponse<Void>> deleteTag(@PathVariable Long tagId) {
        tagService.deleteTag(tagId);
        return ApiResponse.success(null, "Tag deleted successfully");
    }

    @PostMapping("/api/tasks/{taskId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<TaskResponse>> attachTag(
            @PathVariable Long taskId,
            @PathVariable Long tagId) {
        TaskResponse response = tagService.attachTag(taskId, tagId);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/api/tasks/{taskId}/tags/{tagId}")
    public ResponseEntity<ApiResponse<TaskResponse>> detachTag(
            @PathVariable Long taskId,
            @PathVariable Long tagId) {
        TaskResponse response = tagService.detachTag(taskId, tagId);
        return ApiResponse.success(response);
    }
}
