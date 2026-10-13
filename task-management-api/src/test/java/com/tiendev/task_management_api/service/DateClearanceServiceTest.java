package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.request.ProjectUpdateRequest;
import com.tiendev.task_management_api.dto.request.TaskUpdateRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.ProjectRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.service.impl.ProjectServiceImpl;
import com.tiendev.task_management_api.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DateClearanceServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;

    @InjectMocks private ProjectServiceImpl projectService;
    @InjectMocks private TaskServiceImpl taskService;

    private final Long userId = 1L;
    private final Long workspaceId = 10L;

    @BeforeEach
    void setupSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void projectKeepExistingEndDate() {
        Project project = createProject(LocalDate.of(2026, 12, 31));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdateRequest request = ProjectUpdateRequest.builder()
                .name("Updated Name")
                .clearEndDate(false)
                .endDate(null)
                .build();

        projectService.update(100L, request);

        assertEquals(LocalDate.of(2026, 12, 31), project.getEndDate(), "EndDate should remain unchanged");
    }

    @Test
    void projectSetNewEndDate() {
        Project project = createProject(LocalDate.of(2026, 12, 31));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate newDate = LocalDate.of(2027, 1, 15);
        ProjectUpdateRequest request = ProjectUpdateRequest.builder()
                .endDate(newDate)
                .clearEndDate(false)
                .build();

        projectService.update(100L, request);

        assertEquals(newDate, project.getEndDate());
        verify(activityLogService).log(eq(EntityType.PROJECT), eq(100L), eq(ActivityAction.UPDATED),
                eq("endDate"), eq("2026-12-31"), eq("2027-01-15"), eq(userId), eq(workspaceId));
    }

    @Test
    void projectClearEndDate() {
        Project project = createProject(LocalDate.of(2026, 12, 31));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectUpdateRequest request = ProjectUpdateRequest.builder()
                .clearEndDate(true)
                .endDate(null)
                .build();

        projectService.update(100L, request);

        assertNull(project.getEndDate(), "EndDate should be cleared to null");
        verify(activityLogService).log(eq(EntityType.PROJECT), eq(100L), eq(ActivityAction.UPDATED),
                eq("endDate"), eq("2026-12-31"), eq(null), eq(userId), eq(workspaceId));
    }

    @Test
    void projectThrowsWhenBothEndDateAndClearEndDateProvided() {
        Project project = createProject(LocalDate.of(2026, 12, 31));
        when(projectRepository.findById(100L)).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);

        ProjectUpdateRequest request = ProjectUpdateRequest.builder()
                .endDate(LocalDate.of(2027, 1, 1))
                .clearEndDate(true)
                .build();

        assertThrows(BusinessException.class, () -> projectService.update(100L, request));
    }

    @Test
    void taskKeepExistingDeadline() {
        Task task = createTask(LocalDate.of(2026, 11, 20));
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .description("New Description")
                .clearDeadline(false)
                .deadline(null)
                .build();

        taskService.update(200L, request);

        assertEquals(LocalDate.of(2026, 11, 20), task.getDeadline(), "Deadline should remain unchanged");
    }

    @Test
    void taskSetNewDeadline() {
        Task task = createTask(LocalDate.of(2026, 11, 20));
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate newDeadline = LocalDate.of(2026, 11, 25);
        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .deadline(newDeadline)
                .clearDeadline(false)
                .build();

        taskService.update(200L, request);

        assertEquals(newDeadline, task.getDeadline());
        verify(activityLogService).log(eq(EntityType.TASK), eq(200L), eq(ActivityAction.UPDATED),
                eq("deadline"), eq("2026-11-20"), eq("2026-11-25"), eq(userId), eq(workspaceId));
    }

    @Test
    void taskClearDeadline() {
        Task task = createTask(LocalDate.of(2026, 11, 20));
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .clearDeadline(true)
                .deadline(null)
                .build();

        taskService.update(200L, request);

        assertNull(task.getDeadline(), "Deadline should be cleared to null");
        verify(activityLogService).log(eq(EntityType.TASK), eq(200L), eq(ActivityAction.UPDATED),
                eq("deadline"), eq("2026-11-20"), eq(null), eq(userId), eq(workspaceId));
    }

    @Test
    void taskThrowsWhenBothDeadlineAndClearDeadlineProvided() {
        Task task = createTask(LocalDate.of(2026, 11, 20));
        when(taskRepository.findById(200L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(true);
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(workspaceId, userId, WorkspaceRole.OWNER)).thenReturn(true);

        TaskUpdateRequest request = TaskUpdateRequest.builder()
                .deadline(LocalDate.of(2026, 11, 30))
                .clearDeadline(true)
                .build();

        assertThrows(BusinessException.class, () -> taskService.update(200L, request));
    }

    private Project createProject(LocalDate endDate) {
        Workspace ws = new Workspace();
        ws.setId(workspaceId);
        ws.setName("Ws");
        ws.setActive(true);

        Project project = new Project();
        project.setId(100L);
        project.setName("Old Proj");
        project.setWorkspace(ws);
        project.setActive(true);
        project.setEndDate(endDate);
        return project;
    }

    private Task createTask(LocalDate deadline) {
        Project proj = createProject(LocalDate.of(2026, 12, 31));
        Task task = new Task();
        task.setId(200L);
        task.setTitle("Old Task");
        task.setProject(proj);
        task.setActive(true);
        task.setPriority(TaskPriority.MEDIUM);
        task.setStatus(TaskStatus.TODO);
        task.setDeadline(deadline);
        return task;
    }
}
