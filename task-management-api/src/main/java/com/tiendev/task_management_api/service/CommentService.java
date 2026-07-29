package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.CommentResponse;
import com.tiendev.task_management_api.dto.request.CommentCreateRequest;
import com.tiendev.task_management_api.dto.request.CommentUpdateRequest;

import java.util.List;

public interface CommentService {
    CommentResponse create(CommentCreateRequest request);
    CommentResponse getById(Long id);
    List<CommentResponse> getAll();
    List<CommentResponse> getByTaskId(Long taskId);
    CommentResponse update(Long id, CommentUpdateRequest request);
    void delete(Long id);
}
