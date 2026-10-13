package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.ChecklistItemResponse;
import com.tiendev.task_management_api.dto.request.ChecklistItemCreateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistItemUpdateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistReorderRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.TaskChecklistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/checklist")
@RequiredArgsConstructor
public class TaskChecklistController {

    private final TaskChecklistService checklistService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> create(
            @PathVariable Long taskId,
            @Valid @RequestBody ChecklistItemCreateRequest request) {
        ChecklistItemResponse response = checklistService.addChecklistItem(taskId, request);
        return ApiResponse.created(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> getChecklist(@PathVariable Long taskId) {
        List<ChecklistItemResponse> responses = checklistService.getChecklist(taskId);
        return ApiResponse.success(responses);
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<ApiResponse<ChecklistItemResponse>> update(
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @Valid @RequestBody ChecklistItemUpdateRequest request) {
        ChecklistItemResponse response = checklistService.updateChecklistItem(taskId, itemId, request);
        return ApiResponse.success(response);
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long taskId,
            @PathVariable Long itemId) {
        checklistService.deleteChecklistItem(taskId, itemId);
        return ApiResponse.success(null, "Checklist item deleted");
    }

    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<List<ChecklistItemResponse>>> reorder(
            @PathVariable Long taskId,
            @Valid @RequestBody ChecklistReorderRequest request) {
        List<ChecklistItemResponse> responses = checklistService.reorderChecklist(taskId, request);
        return ApiResponse.success(responses);
    }
}
