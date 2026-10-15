package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.WorkspaceMemberResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberUpdateRequest;
import com.tiendev.task_management_api.event.WorkspaceMemberAddedEvent;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.service.ActivityLogService;
import com.tiendev.task_management_api.service.NotificationService;
import com.tiendev.task_management_api.service.WorkspaceMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public WorkspaceMemberResponse create(WorkspaceMemberCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + request.getWorkspaceId()));
        validateOwnerOrAdmin(request.getWorkspaceId());

        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new InvalidOperationException("Cannot add member directly as OWNER. Use transfer ownership instead.");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), user.getId())) {
            throw new ResourceAlreadyExistsException("User is already a member of this workspace");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER);

        member = workspaceMemberRepository.save(member);

        Long currentUserId = getCurrentUserId();

        User inviter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));
        eventPublisher.publishEvent(new WorkspaceMemberAddedEvent(
                user.getId(), currentUserId, workspace.getId(), user.getEmail(), inviter.getUsername(),
                workspace.getName(), member.getRole()));

        activityLogService.log(EntityType.WORKSPACE_MEMBER, member.getId(), ActivityAction.MEMBER_ADDED,
                null, null, user.getUsername(), currentUserId, workspace.getId());

        return toWorkspaceMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceMemberResponse getById(Long id) {
        WorkspaceMember member = workspaceMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember not found with id: " + id));
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                member.getWorkspace().getId(), getCurrentUserId())) {
            throw new ResourceNotFoundException("WorkspaceMember not found with id: " + id);
        }
        return toWorkspaceMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getAll() {
        Long currentUserId = getCurrentUserId();
        return workspaceMemberRepository.findActiveWorkspaceMembersByUserId(currentUserId).stream()
                .map(this::toWorkspaceMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse update(Long id, WorkspaceMemberUpdateRequest request) {
        WorkspaceMember member = workspaceMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember not found with id: " + id));
        validateOwnerOrAdmin(member.getWorkspace().getId());

        Long currentUserId = getCurrentUserId();
        boolean isOwner = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(
                member.getWorkspace().getId(), currentUserId, WorkspaceRole.OWNER);

        WorkspaceRole oldRole = member.getRole();
        WorkspaceRole newRole = request.getRole();

        if (newRole == WorkspaceRole.OWNER) {
            throw new InvalidOperationException("Cannot assign OWNER role via update. Use transfer ownership instead.");
        }
        if (oldRole == WorkspaceRole.OWNER && !isOwner) {
            throw new InvalidOperationException("Only workspace owner can change owner role.");
        }

        if (oldRole == WorkspaceRole.OWNER && newRole != WorkspaceRole.OWNER) {
            long ownerCount = workspaceMemberRepository.countByWorkspaceIdAndRole(
                    member.getWorkspace().getId(), WorkspaceRole.OWNER);
            if (ownerCount <= 1) {
                throw new BusinessException(
                        "Cannot change role of the last owner. Workspace must have at least one owner.");
            }
        }

        member.setRole(newRole);
        member = workspaceMemberRepository.save(member);

        if (oldRole != newRole) {
            notificationService.createNotification(
                    "Vai trò của bạn đã thay đổi trong workspace: " + member.getWorkspace().getName(),
                    "Vai trò của bạn trong workspace \"" + member.getWorkspace().getName()
                            + "\" đã thay đổi từ " + oldRole + " thành " + newRole + ".",
                    NotificationType.WORKSPACE_MEMBER_ROLE_CHANGED,
                    member.getUser().getId(),
                    currentUserId,
                    null,
                    null,
                    member.getWorkspace().getId()
            );
        }

        activityLogService.log(EntityType.WORKSPACE_MEMBER, member.getId(), ActivityAction.ROLE_CHANGED,
                "role", oldRole.name(), newRole.name(), currentUserId, member.getWorkspace().getId());

        return toWorkspaceMemberResponse(member);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WorkspaceMember member = workspaceMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember not found with id: " + id));
        validateOwnerOrAdmin(member.getWorkspace().getId());

        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new InvalidOperationException("Cannot remove workspace owner.");
        }

        Long currentUserId = getCurrentUserId();
        Long memberId = member.getId();
        Long workspaceId = member.getWorkspace().getId();
        String username = member.getUser().getUsername();

        workspaceMemberRepository.deleteById(id);

        activityLogService.log(EntityType.WORKSPACE_MEMBER, memberId, ActivityAction.MEMBER_REMOVED,
                null, null, username, currentUserId, workspaceId);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private Set<Long> getMemberWorkspaceIds() {
        Long currentUserId = getCurrentUserId();
        return workspaceMemberRepository.findByUserId(currentUserId).stream()
                .map(m -> m.getWorkspace().getId())
                .collect(Collectors.toSet());
    }

    private void validateOwnerOrAdmin(Long workspaceId) {
        Long currentUserId = getCurrentUserId();
        boolean authorized = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, currentUserId, List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        if (!authorized) {
            throw new InvalidOperationException("Only workspace owner or admin can perform this action.");
        }
    }

    private WorkspaceMemberResponse toWorkspaceMemberResponse(WorkspaceMember member) {
        UserResponse userResponse = UserResponse.builder()
                .id(member.getUser().getId())
                .username(member.getUser().getUsername())
                .email(member.getUser().getEmail())
                .avatar(member.getUser().getAvatar())
                .role(member.getUser().getRole())
                .build();

        return WorkspaceMemberResponse.builder()
                .id(member.getId())
                .workspaceId(member.getWorkspace().getId())
                .user(userResponse)
                .role(member.getRole())
                .joinedAt(member.getJoinedAt())
                .build();
    }
}
