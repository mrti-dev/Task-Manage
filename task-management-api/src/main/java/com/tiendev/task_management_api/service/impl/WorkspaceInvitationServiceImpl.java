package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.WorkspaceInvitationResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceInvitationCreateRequest;
import com.tiendev.task_management_api.event.WorkspaceMemberAddedEvent;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceInvitation;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.model.enums.WorkspaceInvitationStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceInvitationRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.ActivityLogService;
import com.tiendev.task_management_api.service.WorkspaceInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkspaceInvitationServiceImpl implements WorkspaceInvitationService {

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final UserRepository userRepository;
    private final ActivityLogService activityLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public WorkspaceInvitationResponse createInvitation(Long workspaceId, WorkspaceInvitationCreateRequest request) {
        Long currentUserId = getCurrentUserId();
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        validateOwnerOrAdmin(workspaceId, currentUserId);

        if (request.getRole() == WorkspaceRole.OWNER) {
            throw new InvalidOperationException("Cannot invite user as OWNER. Use transfer ownership instead.");
        }

        String inviteEmail = request.getEmail().trim().toLowerCase();
        User recipient = userRepository.findByEmailIgnoreCase(inviteEmail)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for email: " + inviteEmail));
        if (workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, recipient.getId())) {
            throw new ResourceAlreadyExistsException("User is already a member of this workspace");
        }

        User inviter = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

        WorkspaceRole role = request.getRole() != null ? request.getRole() : WorkspaceRole.MEMBER;
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(recipient);
        member.setRole(role);
        WorkspaceMember savedMember = workspaceMemberRepository.save(member);

        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setWorkspace(workspace);
        invitation.setEmail(inviteEmail);
        invitation.setRole(role);
        invitation.setToken(UUID.randomUUID().toString());
        invitation.setStatus(WorkspaceInvitationStatus.ADDED);
        invitation.setInvitedBy(inviter);
        invitation.setExpiresAt(LocalDateTime.now().plusDays(7));

        final WorkspaceInvitation savedInvitation = invitationRepository.save(invitation);
        activityLogService.log(EntityType.WORKSPACE_MEMBER, savedMember.getId(), ActivityAction.MEMBER_ADDED,
                null, null, recipient.getUsername(), currentUserId, workspace.getId());
        eventPublisher.publishEvent(new WorkspaceMemberAddedEvent(
                recipient.getId(), currentUserId, workspace.getId(), recipient.getEmail(), inviter.getUsername(),
                workspace.getName(), role));

        return toResponse(savedInvitation);
    }

    @Override
    @Transactional
    public List<WorkspaceInvitationResponse> getWorkspaceInvitations(Long workspaceId) {
        Long currentUserId = getCurrentUserId();
        validateOwnerOrAdmin(workspaceId, currentUserId);

        List<WorkspaceInvitation> list = invitationRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        LocalDateTime now = LocalDateTime.now();
        for (WorkspaceInvitation inv : list) {
            if (inv.getStatus() == WorkspaceInvitationStatus.PENDING && inv.getExpiresAt().isBefore(now)) {
                inv.setStatus(WorkspaceInvitationStatus.EXPIRED);
                invitationRepository.save(inv);
            }
        }

        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public WorkspaceInvitationResponse resendInvitation(Long invitationId) {
        Long currentUserId = getCurrentUserId();
        WorkspaceInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found with id: " + invitationId));

        validateOwnerOrAdmin(invitation.getWorkspace().getId(), currentUserId);

        if (invitation.getStatus() != WorkspaceInvitationStatus.PENDING
                && invitation.getStatus() != WorkspaceInvitationStatus.EXPIRED
                && invitation.getStatus() != WorkspaceInvitationStatus.ADDED
                && invitation.getStatus() != WorkspaceInvitationStatus.ACCEPTED) {
            throw new InvalidOperationException("Only active workspace members can be notified again");
        }

        User recipient = userRepository.findByEmailIgnoreCase(invitation.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("No account found for email: " + invitation.getEmail()));
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(invitation.getWorkspace().getId(), recipient.getId())) {
            WorkspaceMember member = new WorkspaceMember();
            member.setWorkspace(invitation.getWorkspace());
            member.setUser(recipient);
            member.setRole(invitation.getRole());
            WorkspaceMember savedMember = workspaceMemberRepository.save(member);
            activityLogService.log(EntityType.WORKSPACE_MEMBER, savedMember.getId(), ActivityAction.MEMBER_ADDED,
                    null, null, recipient.getUsername(), currentUserId, invitation.getWorkspace().getId());
        }

        invitation.setStatus(WorkspaceInvitationStatus.ADDED);
        WorkspaceInvitation savedInvitation = invitationRepository.save(invitation);

        String inviterName = savedInvitation.getInvitedBy() != null ? savedInvitation.getInvitedBy().getUsername() : "Admin";
        eventPublisher.publishEvent(new WorkspaceMemberAddedEvent(
                recipient.getId(), currentUserId, savedInvitation.getWorkspace().getId(), recipient.getEmail(),
                inviterName, savedInvitation.getWorkspace().getName(), savedInvitation.getRole()));

        return toResponse(savedInvitation);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private void validateOwnerOrAdmin(Long workspaceId, Long userId) {
        boolean authorized = workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                workspaceId, userId, List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN));
        if (!authorized) {
            throw new InvalidOperationException("Only workspace owner or admin can perform this action.");
        }
    }

    private WorkspaceInvitationResponse toResponse(WorkspaceInvitation invitation) {
        return WorkspaceInvitationResponse.builder()
                .id(invitation.getId())
                .workspaceId(invitation.getWorkspace().getId())
                .workspaceName(invitation.getWorkspace().getName())
                .email(invitation.getEmail())
                .role(invitation.getRole())
                .status(invitation.getStatus())
                .invitedByName(invitation.getInvitedBy() != null ? invitation.getInvitedBy().getUsername() : null)
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .build();
    }

}
