package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.CommentResponse;
import com.tiendev.task_management_api.dto.request.CommentCreateRequest;
import com.tiendev.task_management_api.dto.request.CommentUpdateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(@Valid @RequestBody CommentCreateRequest request) {
        CommentResponse response = commentService.create(request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getAll() {
        List<CommentResponse> responses = commentService.getAll();
        return ApiResponse.success(responses);
    }

    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getByTaskId(@PathVariable Long taskId) {
        List<CommentResponse> responses = commentService.getByTaskId(taskId);
        return ApiResponse.success(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> getById(@PathVariable Long id) {
        CommentResponse response = commentService.getById(id);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CommentResponse>> update(@PathVariable Long id, @Valid @RequestBody CommentUpdateRequest request) {
        CommentResponse response = commentService.update(id, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ApiResponse.success(null, "Comment deleted successfully");
    }
}
