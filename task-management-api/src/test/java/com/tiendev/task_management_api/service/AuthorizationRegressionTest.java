package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.repository.*;
import com.tiendev.task_management_api.service.impl.ActivityLogServiceImpl;
import com.tiendev.task_management_api.service.impl.CommentServiceImpl;
import com.tiendev.task_management_api.service.impl.WorkspaceMemberServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationRegressionTest {

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;
    @Mock private CommentRepository commentRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private ActivityLogRepository activityLogRepository;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private WorkspaceMemberServiceImpl workspaceMemberService;
    @InjectMocks private CommentServiceImpl commentService;
    @InjectMocks private ActivityLogServiceImpl activityLogServiceImpl;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hidesWorkspaceMemberFromNonMember() {
        authenticate(7L);
        Workspace workspace = workspace(10L);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        when(workspaceMemberRepository.findById(3L)).thenReturn(Optional.of(member));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> workspaceMemberService.getById(3L));
    }

    @Test
    void blocksCommentsFromAnotherWorkspace() {
        authenticate(7L);
        Workspace workspace = workspace(10L);
        Project project = new Project();
        project.setWorkspace(workspace);
        Task task = new Task();
        task.setProject(project);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 7L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> commentService.getByTaskId(20L));
        verify(commentRepository, never()).findByTaskId(20L);
    }

    @Test
    void scopesEntityActivityToMemberWorkspaces() {
        authenticate(7L);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace(10L));
        PageRequest pageable = PageRequest.of(0, 20);
        when(workspaceMemberRepository.findByUserId(7L)).thenReturn(List.of(member));
        when(activityLogRepository.findByWorkspaceIdInAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                Set.of(10L), EntityType.TASK, 20L, pageable)).thenReturn(Page.empty(pageable));

        activityLogServiceImpl.getByEntity(EntityType.TASK, 20L, pageable);

        verify(activityLogRepository).findByWorkspaceIdInAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
                Set.of(10L), EntityType.TASK, 20L, pageable);
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null));
    }

    private Workspace workspace(Long id) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        return workspace;
    }
}
