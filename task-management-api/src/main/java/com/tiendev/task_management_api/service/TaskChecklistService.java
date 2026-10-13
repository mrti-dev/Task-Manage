package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.ChecklistItemResponse;
import com.tiendev.task_management_api.dto.request.ChecklistItemCreateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistItemUpdateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistReorderRequest;

import java.util.List;

public interface TaskChecklistService {

    ChecklistItemResponse addChecklistItem(Long taskId, ChecklistItemCreateRequest request);

    ChecklistItemResponse updateChecklistItem(Long taskId, Long itemId, ChecklistItemUpdateRequest request);

    void deleteChecklistItem(Long taskId, Long itemId);

    List<ChecklistItemResponse> reorderChecklist(Long taskId, ChecklistReorderRequest request);

    List<ChecklistItemResponse> getChecklist(Long taskId);
}
