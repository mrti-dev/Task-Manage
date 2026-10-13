package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.SavedFilterResponse;
import com.tiendev.task_management_api.dto.request.SavedFilterCreateRequest;
import com.tiendev.task_management_api.dto.request.SavedFilterUpdateRequest;

import java.util.List;

public interface SavedFilterService {

    SavedFilterResponse create(SavedFilterCreateRequest request);

    List<SavedFilterResponse> getAll(Long workspaceId);

    SavedFilterResponse update(Long id, SavedFilterUpdateRequest request);

    void delete(Long id);
}
