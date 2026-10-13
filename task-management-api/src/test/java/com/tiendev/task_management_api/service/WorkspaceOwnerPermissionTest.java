package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.dto.request.WorkspaceMemberCreateRequest;
import com.tiendev.task_management_api.event.WorkspaceMemberAddedEvent;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.ActivityLogRepository;
import com.tiendev.task_management_api.repository.ProjectRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.impl.ProjectServiceImpl;
import com.tiendev.task_management_api.service.impl.TaskServiceImpl;
import com.tiendev.task_management_api.service.impl.WorkspaceMemberServiceImpl;
import com.tiendev.task_management_api.service.impl.WorkspaceServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkspaceOwnerPermissionTest {

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private WorkspaceServiceImpl workspaceService;
    @InjectMocks private WorkspaceMemberServiceImpl workspaceMemberService;
    @InjectMocks private ProjectServiceImpl projectService;
    @InjectMocks private TaskServiceImpl taskService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsBothOwnersWhenWorkspaceHasTwoOwners() {
        Long workspaceId = 100L;
        Long owner1Id = 1L;
        Long owner2Id = 2L;

        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, owner1Id, WorkspaceRole.OWNER))
                .thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, owner2Id, WorkspaceRole.OWNER))
                .thenReturn(true);

        // Owner 1 deletes an active workspace
        Workspace workspace1 = new Workspace();
        workspace1.setId(workspaceId);
        workspace1.setActive(true);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace1));

        authenticate(owner1Id);
        assertDoesNotThrow(() -> workspaceService.delete(workspaceId));

        // Owner 2 also can delete an active workspace
        Workspace workspace2 = new Workspace();
        workspace2.setId(workspaceId);
        workspace2.setActive(true);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace2));

        authenticate(owner2Id);
        assertDoesNotThrow(() -> workspaceService.delete(workspaceId));
    }

    @Test
    void deniesRegularMemberFromPerformingOwnerAction() {
        Long workspaceId = 100L;
        Long memberId = 3L;

        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setActive(true);

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, memberId, WorkspaceRole.OWNER))
                .thenReturn(false);

        authenticate(memberId);
        assertThrows(InvalidOperationException.class, () -> workspaceService.delete(workspaceId));
    }

    @Test
    void directMemberAddQueuesAccessNoticeEvent() {
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        workspace.setName("Design Team");
        User inviter = createUser(1L);
        User recipient = createUser(2L);

        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(workspace));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRoleIn(
                10L, 1L, List.of(WorkspaceRole.OWNER, WorkspaceRole.ADMIN))).thenReturn(true);
        when(userRepository.findByEmail("user2@example.com")).thenReturn(Optional.of(recipient));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 2L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(inviter));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(call -> {
            WorkspaceMember member = call.getArgument(0);
            member.setId(20L);
            return member;
        });
        authenticate(1L);

        workspaceMemberService.create(new WorkspaceMemberCreateRequest(10L, "user2@example.com", WorkspaceRole.MEMBER));

        verify(eventPublisher).publishEvent(argThat((Object event) -> event instanceof WorkspaceMemberAddedEvent added
                && added.recipientId().equals(2L) && added.workspaceId().equals(10L)));
    }

    @Test
    void doesNotThrowWhenFindingMembersAndMultipleOwnersExist() {
        Long workspaceId = 100L;
        WorkspaceMember owner1 = createMember(1L, 10L, WorkspaceRole.OWNER);
        WorkspaceMember owner2 = createMember(2L, 20L, WorkspaceRole.OWNER);

        when(workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER))
                .thenReturn(List.of(owner1, owner2));

        List<WorkspaceMember> owners = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER);
        org.junit.jupiter.api.Assertions.assertEquals(2, owners.size());
    }

    @Test
    void sendsNotificationToAllOwnersInProjectNotification() {
        Long workspaceId = 100L;
        Long senderId = 5L;

        Workspace workspace = new Workspace();
        workspace.setId(workspaceId);
        workspace.setName("Ws");
        workspace.setActive(true);

        Project project = new Project();
        project.setId(10L);
        project.setName("Proj");
        project.setWorkspace(workspace);
        project.setActive(true);

        User userOwner1 = createUser(1L);
        User userOwner2 = createUser(2L);
        User userMember = createUser(5L);

        WorkspaceMember m1 = createMember(1L, userOwner1, WorkspaceRole.OWNER);
        WorkspaceMember m2 = createMember(2L, userOwner2, WorkspaceRole.OWNER);
        WorkspaceMember m3 = createMember(3L, userMember, WorkspaceRole.MEMBER);

        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, senderId, WorkspaceRole.OWNER)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenReturn(project);
        when(workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER))
                .thenReturn(List.of(m1, m2));
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId))
                .thenReturn(List.of(m1, m2, m3));

        authenticate(senderId);
        com.tiendev.task_management_api.dto.request.ProjectUpdateRequest updateReq =
                com.tiendev.task_management_api.dto.request.ProjectUpdateRequest.builder()
                        .name("Updated Name")
                        .build();
        projectService.update(10L, updateReq);

        // Verify notification sent to both owner 1 and owner 2
        verify(notificationService).createNotification(
                any(), any(), eq(NotificationType.PROJECT_UPDATED),
                eq(1L), eq(senderId), any(), eq(10L), eq(workspaceId));
        verify(notificationService).createNotification(
                any(), any(), eq(NotificationType.PROJECT_UPDATED),
                eq(2L), eq(senderId), any(), eq(10L), eq(workspaceId));
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null));
    }

    private User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setEmail("user" + id + "@example.com");
        return user;
    }

    private WorkspaceMember createMember(Long id, Long userId, WorkspaceRole role) {
        return createMember(id, createUser(userId), role);
    }

    private WorkspaceMember createMember(Long id, User user, WorkspaceRole role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setId(id);
        member.setUser(user);
        member.setRole(role);
        return member;
    }
}
