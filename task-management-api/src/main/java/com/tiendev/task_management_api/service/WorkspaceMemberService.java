package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.WorkspaceMemberResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberUpdateRequest;

import java.util.List;

public interface WorkspaceMemberService {
    WorkspaceMemberResponse create(WorkspaceMemberCreateRequest request);
    WorkspaceMemberResponse getById(Long id);
    List<WorkspaceMemberResponse> getAll();
    WorkspaceMemberResponse update(Long id, WorkspaceMemberUpdateRequest request);
    void delete(Long id);
}
