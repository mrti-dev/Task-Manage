package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.ChecklistItemResponse;
import com.tiendev.task_management_api.dto.request.ChecklistItemCreateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistItemUpdateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistReorderRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.TaskChecklistItem;
import com.tiendev.task_management_api.repository.TaskChecklistItemRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.service.TaskChecklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskChecklistServiceImpl implements TaskChecklistService {

    private final TaskChecklistItemRepository checklistItemRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public ChecklistItemResponse addChecklistItem(Long taskId, ChecklistItemCreateRequest request) {
        Task task = getTaskAndValidateAccess(taskId);
        int maxPosition = checklistItemRepository.findByTaskIdOrderByPositionAsc(taskId).stream()
                .mapToInt(TaskChecklistItem::getPosition)
                .max()
                .orElse(-1);

        TaskChecklistItem item = new TaskChecklistItem();
        item.setTask(task);
        item.setTitle(request.getTitle().trim());
        item.setCompleted(false);
        item.setPosition(maxPosition + 1);

        item = checklistItemRepository.save(item);
        broadcastChecklistUpdate(task);
        return toResponse(item);
    }

    @Override
    @Transactional
    public ChecklistItemResponse updateChecklistItem(Long taskId, Long itemId, ChecklistItemUpdateRequest request) {
        Task task = getTaskAndValidateAccess(taskId);
        TaskChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found with id: " + itemId));

        if (!item.getTask().getId().equals(taskId)) {
            throw new InvalidOperationException("Checklist item does not belong to this task");
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            item.setTitle(request.getTitle().trim());
        }
        if (request.getCompleted() != null) {
            item.setCompleted(request.getCompleted());
        }

        item = checklistItemRepository.save(item);
        broadcastChecklistUpdate(task);
        return toResponse(item);
    }

    @Override
    @Transactional
    public void deleteChecklistItem(Long taskId, Long itemId) {
        Task task = getTaskAndValidateAccess(taskId);
        TaskChecklistItem item = checklistItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Checklist item not found with id: " + itemId));

        if (!item.getTask().getId().equals(taskId)) {
            throw new InvalidOperationException("Checklist item does not belong to this task");
        }

        checklistItemRepository.delete(item);
        broadcastChecklistUpdate(task);
    }

    @Override
    @Transactional
    public List<ChecklistItemResponse> reorderChecklist(Long taskId, ChecklistReorderRequest request) {
        Task task = getTaskAndValidateAccess(taskId);
        List<TaskChecklistItem> items = checklistItemRepository.findByTaskIdOrderByPositionAsc(taskId);
        List<Long> order = request.getItemIds();
        Set<Long> requestedIds = new HashSet<>(order);
        Set<Long> existingIds = items.stream().map(TaskChecklistItem::getId).collect(java.util.stream.Collectors.toSet());
        if (requestedIds.size() != order.size() || !requestedIds.equals(existingIds)) {
            throw new BusinessException("Checklist order must contain each item exactly once.");
        }

        for (TaskChecklistItem item : items) {
            int newPos = order.indexOf(item.getId());
            if (newPos != -1) {
                item.setPosition(newPos);
                checklistItemRepository.save(item);
            }
        }

        broadcastChecklistUpdate(task);
        return checklistItemRepository.findByTaskIdOrderByPositionAsc(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChecklistItemResponse> getChecklist(Long taskId) {
        getTaskAndValidateAccess(taskId);
        return checklistItemRepository.findByTaskIdOrderByPositionAsc(taskId).stream()
                .map(this::toResponse)
                .toList();
    }

    private Task getTaskAndValidateAccess(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        Long currentUserId = getCurrentUserId();
        Long workspaceId = task.getProject().getWorkspace().getId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserId)) {
            throw new InvalidOperationException("You do not have access to this workspace");
        }
        return task;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private void broadcastChecklistUpdate(Task task) {
        try {
            List<ChecklistItemResponse> items = checklistItemRepository
                    .findByTaskIdOrderByPositionAsc(task.getId()).stream()
                    .map(this::toResponse)
                    .toList();
            Long workspaceId = task.getProject().getWorkspace().getId();
            messagingTemplate.convertAndSend(
                    "/topic/workspace/" + workspaceId + "/tasks/" + task.getId() + "/checklist", items);
        } catch (Exception ignored) {
        }
    }

    private ChecklistItemResponse toResponse(TaskChecklistItem item) {
        return ChecklistItemResponse.builder()
                .id(item.getId())
                .taskId(item.getTask().getId())
                .title(item.getTitle())
                .completed(item.isCompleted())
                .position(item.getPosition())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
