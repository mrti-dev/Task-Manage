package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.ActivityLogResponse;
import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.ActivityLog;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.repository.ActivityLogRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public void log(EntityType entityType, Long entityId, ActivityAction action,
                    String fieldName, String oldValue, String newValue,
                    Long performedBy, Long workspaceId) {
        User user = userRepository.findById(performedBy)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + performedBy));
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        ActivityLog log = new ActivityLog();
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setAction(action);
        log.setFieldName(fieldName);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setPerformedBy(user);
        log.setWorkspace(workspace);

        activityLogRepository.save(log);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> getByEntity(EntityType entityType, Long entityId, Pageable pageable) {
        Page<ActivityLog> page = activityLogRepository
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(entityType, entityId, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ActivityLogResponse> getByWorkspaceId(Long workspaceId, Pageable pageable) {
        Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
        if (!memberWorkspaceIds.contains(workspaceId)) {
            throw new ResourceNotFoundException("Workspace not found with id: " + workspaceId);
        }
        Page<ActivityLog> page = activityLogRepository
                .findByWorkspaceIdInOrderByCreatedAtDesc(Set.of(workspaceId), pageable);
        return toPageResponse(page);
    }

    private Set<Long> getMemberWorkspaceIds() {
        Long currentUserId = getCurrentUserId();
        return workspaceMemberRepository.findByUserId(currentUserId).stream()
                .map(m -> m.getWorkspace().getId())
                .collect(Collectors.toSet());
    }

    private Long getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private PageResponse<ActivityLogResponse> toPageResponse(Page<ActivityLog> page) {
        List<ActivityLogResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.from(page, content);
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        UserResponse userResponse = UserResponse.builder()
                .id(log.getPerformedBy().getId())
                .username(log.getPerformedBy().getUsername())
                .email(log.getPerformedBy().getEmail())
                .role(log.getPerformedBy().getRole())
                .build();

        return ActivityLogResponse.builder()
                .id(log.getId())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .action(log.getAction())
                .fieldName(log.getFieldName())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .performedBy(userResponse)
                .workspaceId(log.getWorkspace().getId())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
