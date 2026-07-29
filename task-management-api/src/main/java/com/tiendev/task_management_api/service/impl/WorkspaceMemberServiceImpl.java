package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.WorkspaceMemberResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberUpdateRequest;
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

    @Override
    @Transactional
    public WorkspaceMemberResponse create(WorkspaceMemberCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + request.getWorkspaceId()));
        validateOwner(request.getWorkspaceId());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), user.getId())) {
            throw new ResourceAlreadyExistsException("User is already a member of this workspace");
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(request.getRole());

        member = workspaceMemberRepository.save(member);

        Long currentUserId = getCurrentUserId();

        notificationService.createNotification(
                "Bạn đã được thêm vào workspace: " + workspace.getName(),
                "Bạn đã được thêm vào workspace \"" + workspace.getName() + "\" với vai trò " + request.getRole() + ".",
                NotificationType.valueOf("WORKSPACE_MEMBER_ADDED"),
                user.getId(),
                currentUserId,
                null,
                null,
                workspace.getId()
        );

        activityLogService.log(EntityType.WORKSPACE_MEMBER, member.getId(), ActivityAction.MEMBER_ADDED,
                null, null, user.getUsername(), currentUserId, workspace.getId());

        return toWorkspaceMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceMemberResponse getById(Long id) {
        WorkspaceMember member = workspaceMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember not found with id: " + id));
        return toWorkspaceMemberResponse(member);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceMemberResponse> getAll() {
        Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
        return workspaceMemberRepository.findAll().stream()
                .filter(m -> memberWorkspaceIds.contains(m.getWorkspace().getId()))
                .map(this::toWorkspaceMemberResponse)
                .toList();
    }

    @Override
    @Transactional
    public WorkspaceMemberResponse update(Long id, WorkspaceMemberUpdateRequest request) {
        WorkspaceMember member = workspaceMemberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkspaceMember not found with id: " + id));
        validateOwner(member.getWorkspace().getId());

        WorkspaceRole oldRole = member.getRole();
        WorkspaceRole newRole = request.getRole();

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

        Long currentUserId = getCurrentUserId();

        if (oldRole != newRole) {
            notificationService.createNotification(
                    "Vai trò của bạn đã thay đổi trong workspace: " + member.getWorkspace().getName(),
                    "Vai trò của bạn trong workspace \"" + member.getWorkspace().getName()
                            + "\" đã thay đổi từ " + oldRole + " thành " + newRole + ".",
                    NotificationType.valueOf("WORKSPACE_MEMBER_ROLE_CHANGED"),
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
        validateOwner(member.getWorkspace().getId());

        if (member.getRole() == WorkspaceRole.OWNER) {
            long ownerCount = workspaceMemberRepository.countByWorkspaceIdAndRole(
                    member.getWorkspace().getId(), WorkspaceRole.OWNER);
            if (ownerCount <= 1) {
                throw new BusinessException(
                        "Cannot remove the last owner. Workspace must have at least one owner.");
            }
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

    private void validateOwner(Long workspaceId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(auth.getName());
        boolean isOwner = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
                .filter(owner -> owner.getUser().getId().equals(currentUserId))
                .isPresent();
        if (!isOwner) {
            throw new InvalidOperationException("Only workspace owner can perform this action.");
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
