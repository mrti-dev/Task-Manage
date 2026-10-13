package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.SavedFilterResponse;
import com.tiendev.task_management_api.dto.request.SavedFilterCreateRequest;
import com.tiendev.task_management_api.dto.request.SavedFilterUpdateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.SavedFilter;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.repository.SavedFilterRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.SavedFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SavedFilterServiceImpl implements SavedFilterService {

    private final SavedFilterRepository savedFilterRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;

    @Override
    @Transactional
    public SavedFilterResponse create(SavedFilterCreateRequest request) {
        Long currentUserId = getCurrentUserId();
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        Workspace workspace = null;
        if (request.getWorkspaceId() != null) {
            workspace = workspaceRepository.findById(request.getWorkspaceId()).orElse(null);
        }

        SavedFilter filter = new SavedFilter();
        filter.setUser(user);
        filter.setWorkspace(workspace);
        filter.setName(request.getName().trim());
        filter.setCriteriaJson(request.getCriteriaJson());

        filter = savedFilterRepository.save(filter);
        return toResponse(filter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedFilterResponse> getAll(Long workspaceId) {
        Long currentUserId = getCurrentUserId();
        List<SavedFilter> list;
        if (workspaceId != null) {
            list = savedFilterRepository.findByUserIdAndWorkspaceIdOrderByCreatedAtDesc(currentUserId, workspaceId);
        } else {
            list = savedFilterRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        }
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SavedFilterResponse update(Long id, SavedFilterUpdateRequest request) {
        Long currentUserId = getCurrentUserId();
        SavedFilter filter = savedFilterRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved filter not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            filter.setName(request.getName().trim());
        }
        if (request.getCriteriaJson() != null && !request.getCriteriaJson().isBlank()) {
            filter.setCriteriaJson(request.getCriteriaJson());
        }

        filter = savedFilterRepository.save(filter);
        return toResponse(filter);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Long currentUserId = getCurrentUserId();
        SavedFilter filter = savedFilterRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Saved filter not found with id: " + id));

        savedFilterRepository.delete(filter);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private SavedFilterResponse toResponse(SavedFilter filter) {
        return SavedFilterResponse.builder()
                .id(filter.getId())
                .name(filter.getName())
                .criteriaJson(filter.getCriteriaJson())
                .workspaceId(filter.getWorkspace() != null ? filter.getWorkspace().getId() : null)
                .createdAt(filter.getCreatedAt())
                .updatedAt(filter.getUpdatedAt())
                .build();
    }
}
