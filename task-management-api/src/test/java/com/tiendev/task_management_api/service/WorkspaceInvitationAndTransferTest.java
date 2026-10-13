package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.request.TransferOwnershipRequest;
import com.tiendev.task_management_api.dto.WorkspaceInvitationResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceInvitationCreateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceInvitation;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.WorkspaceInvitationStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceInvitationRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.impl.WorkspaceInvitationServiceImpl;
import com.tiendev.task_management_api.service.impl.WorkspaceServiceImpl;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationAndTransferTest {

    @Mock private WorkspaceInvitationRepository invitationRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private WorkspaceInvitationServiceImpl invitationService;
    @InjectMocks private WorkspaceServiceImpl workspaceService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsInviteIfUserAlreadyMember() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(10L, 1L, List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN)))
                .thenReturn(true);

        User member = new User();
        member.setId(2L);
        member.setEmail("member@test.com");
        when(userRepository.findByEmailIgnoreCase("member@test.com")).thenReturn(Optional.of(member));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(true);

        WorkspaceInvitationCreateRequest req = WorkspaceInvitationCreateRequest.builder()
                .email("member@test.com")
                .role(WorkspaceRole.MEMBER)
                .build();

        assertThrows(ResourceAlreadyExistsException.class, () -> invitationService.createInvitation(10L, req));
    }

    @Test
    void addsExistingAccountImmediatelyAndPublishesNotice() {
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("Design Team");
        User owner = new User();
        owner.setId(1L);
        owner.setUsername("owner");
        User recipient = new User();
        recipient.setId(2L);
        recipient.setUsername("member");
        recipient.setEmail("member@test.com");

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                10L, 1L, List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN))).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("member@test.com")).thenReturn(Optional.of(recipient));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
                .thenAnswer(call -> call.getArgument(0));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(invitationRepository.save(any(WorkspaceInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));

        WorkspaceInvitationResponse result = invitationService.createInvitation(10L,
                new WorkspaceInvitationCreateRequest("member@test.com", WorkspaceRole.MEMBER));

        assertEquals(WorkspaceInvitationStatus.ADDED, result.getStatus());
        verify(workspaceMemberRepository).save(argThat(member -> member.getUser() == recipient
                && member.getWorkspace() == workspace && member.getRole() == WorkspaceRole.MEMBER));
        verify(eventPublisher).publishEvent(argThat((Object event) -> event instanceof com.tiendev.task_management_api.event.WorkspaceMemberAddedEvent added
                && added.recipientId().equals(2L) && added.workspaceId().equals(10L)));
    }

    @Test
    void resendAddsExpiredInvitationRecipientImmediately() {
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        User recipient = new User();
        recipient.setId(2L);
        recipient.setEmail("invitee@test.com");
        WorkspaceInvitation invitation = new WorkspaceInvitation();
        invitation.setId(100L);
        invitation.setWorkspace(workspace);
        invitation.setEmail("invitee@test.com");
        invitation.setToken("expired-token");
        invitation.setStatus(WorkspaceInvitationStatus.EXPIRED);
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(invitationRepository.findById(100L)).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                10L, 1L, List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN))).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("invitee@test.com")).thenReturn(Optional.of(recipient));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(call -> call.getArgument(0));
        when(invitationRepository.save(any(WorkspaceInvitation.class)))
                .thenAnswer(call -> call.getArgument(0));

        WorkspaceInvitationResponse resent = invitationService.resendInvitation(100L);

        assertEquals(WorkspaceInvitationStatus.ADDED, resent.getStatus());
        assertNotNull(resent.getExpiresAt());
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    void transferOwnershipSuccessfullyEnsuresExactlyOneOwner() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        ws.setActive(true);
        when(workspaceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(10L, 1L, WorkspaceRole.OWNER)).thenReturn(true);

        User oldOwner = new User();
        oldOwner.setId(1L);
        WorkspaceMember oldOwnerMember = new WorkspaceMember();
        oldOwnerMember.setId(101L);
        oldOwnerMember.setUser(oldOwner);
        oldOwnerMember.setRole(WorkspaceRole.OWNER);

        User newOwner = new User();
        newOwner.setId(2L);
        WorkspaceMember newOwnerMember = new WorkspaceMember();
        newOwnerMember.setId(102L);
        newOwnerMember.setUser(newOwner);
        newOwnerMember.setRole(WorkspaceRole.MEMBER);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 1L)).thenReturn(Optional.of(oldOwnerMember));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(10L, 2L)).thenReturn(Optional.of(newOwnerMember));
        when(workspaceMemberRepository.countByWorkspaceIdAndRole(10L, WorkspaceRole.OWNER)).thenReturn(1L);

        TransferOwnershipRequest req = new TransferOwnershipRequest(2L);
        workspaceService.transferOwnership(10L, req);

        assertEquals(WorkspaceRole.ADMIN, oldOwnerMember.getRole());
        assertEquals(WorkspaceRole.OWNER, newOwnerMember.getRole());
        verify(workspaceMemberRepository).save(oldOwnerMember);
        verify(workspaceMemberRepository).save(newOwnerMember);
    }

    @Test
    void transferOwnershipRejectsSelfTransfer() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        ws.setActive(true);
        when(workspaceRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(10L, 1L, WorkspaceRole.OWNER)).thenReturn(true);

        TransferOwnershipRequest req = new TransferOwnershipRequest(1L);
        assertThrows(InvalidOperationException.class, () -> workspaceService.transferOwnership(10L, req));
    }

    @Test
    void creatingWorkspaceAlwaysCreatesExactlyOneOwner() {
        User creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        Workspace savedWs = new Workspace();
        savedWs.setId(50L);
        savedWs.setName("Test WS");
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(savedWs);

        WorkspaceCreateRequest req = new WorkspaceCreateRequest();
        req.setName("Test WS");

        workspaceService.create(req);

        // Verify exactly one owner member was added to workspace
        assertEquals(1, savedWs.getMembers().size());
        assertEquals(WorkspaceRole.OWNER, savedWs.getMembers().get(0).getRole());
        assertEquals(creator, savedWs.getMembers().get(0).getUser());
    }
}
