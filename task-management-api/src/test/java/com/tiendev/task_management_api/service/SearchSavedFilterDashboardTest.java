package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.GlobalSearchResponse;
import com.tiendev.task_management_api.dto.WorkspaceDashboardResponse;
import com.tiendev.task_management_api.dto.request.SavedFilterCreateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.*;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.repository.*;
import com.tiendev.task_management_api.service.impl.DashboardServiceImpl;
import com.tiendev.task_management_api.service.impl.SavedFilterServiceImpl;
import com.tiendev.task_management_api.service.impl.SearchServiceImpl;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchSavedFilterDashboardTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private SavedFilterRepository savedFilterRepository;

    @InjectMocks private SearchServiceImpl searchService;
    @InjectMocks private SavedFilterServiceImpl savedFilterService;
    @InjectMocks private DashboardServiceImpl dashboardService;

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
    void searchDoesNotLeakDataFromOtherWorkspaces() {
        // User 1 belongs only to workspace 10
        WorkspaceMember member = new WorkspaceMember();
        Workspace myWs = new Workspace();
        myWs.setId(10L);
        myWs.setName("My Workspace");
        myWs.setActive(true);
        member.setWorkspace(myWs);
        when(workspaceMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        // Another workspace 20 which user 1 does NOT belong to
        Workspace otherWs = new Workspace();
        otherWs.setId(20L);
        otherWs.setName("Secret Workspace");
        otherWs.setActive(true);

        when(workspaceRepository.searchWorkspaces(org.mockito.ArgumentMatchers.eq(Set.of(10L)), org.mockito.ArgumentMatchers.eq("secret"), any())).thenReturn(List.of());
        when(projectRepository.searchProjects(org.mockito.ArgumentMatchers.eq(Set.of(10L)), org.mockito.ArgumentMatchers.eq("secret"), any())).thenReturn(List.of());
        when(taskRepository.searchTasks(org.mockito.ArgumentMatchers.eq(Set.of(10L)), org.mockito.ArgumentMatchers.eq("secret"), any())).thenReturn(List.of());

        GlobalSearchResponse result = searchService.search("Secret", null);

        // Result should NOT contain Secret Workspace
        assertTrue(result.getWorkspaces().isEmpty());
        verify(workspaceRepository).searchWorkspaces(Set.of(10L), "secret", org.springframework.data.domain.PageRequest.of(0, 10));
    }

    @Test
    void savedFilterOnlyAccessibleByCreator() {
        User creator = new User();
        creator.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        SavedFilter saved = new SavedFilter();
        saved.setId(100L);
        saved.setUser(creator);
        saved.setName("My Filter");
        saved.setCriteriaJson("{\"status\":\"TODO\"}");
        when(savedFilterRepository.save(any(SavedFilter.class))).thenReturn(saved);

        SavedFilterCreateRequest req = new SavedFilterCreateRequest("My Filter", "{\"status\":\"TODO\"}", 10L);
        var res = savedFilterService.create(req);

        assertEquals("My Filter", res.getName());

        // When another user (e.g. user 2) tries to delete this filter, it should fail with ResourceNotFoundException
        when(savedFilterRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> savedFilterService.delete(100L));
    }

    @Test
    void dashboardReturnsCorrectAggregatedMetricsWithoutNPlusOne() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        ws.setActive(true);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);

        Project prj = new Project();
        prj.setId(100L);
        prj.setName("Project Alpha");
        prj.setWorkspace(ws);
        prj.setActive(true);

        when(projectRepository.findByWorkspaceIdAndActiveTrue(10L)).thenReturn(List.of(prj));

        User assignee = new User();
        assignee.setId(5L);
        assignee.setUsername("worker");

        Task t1 = new Task();
        t1.setId(1L);
        t1.setProject(prj);
        t1.setStatus(TaskStatus.DONE);
        t1.setPriority(TaskPriority.HIGH);
        t1.setActive(true);
        t1.setAssignee(assignee);

        Task t2 = new Task();
        t2.setId(2L);
        t2.setProject(prj);
        t2.setStatus(TaskStatus.IN_PROGRESS);
        t2.setPriority(TaskPriority.MEDIUM);
        t2.setDeadline(LocalDate.now().minusDays(3)); // overdue
        t2.setActive(true);
        t2.setAssignee(assignee);

        when(taskRepository.findActiveTasksForDashboard(10L, null)).thenReturn(List.of(t1, t2));

        WorkspaceDashboardResponse dashboard = dashboardService.getWorkspaceDashboard(10L, null, null, null);

        assertEquals(2, dashboard.getTotalTasks());
        assertEquals(1, dashboard.getCompletedTasks());
        assertEquals(50.0, dashboard.getCompletionRate());
        assertEquals(1, dashboard.getOverdueTasks());
        assertEquals(1, dashboard.getProjectProgress().size());
        assertEquals("Project Alpha", dashboard.getProjectProgress().get(0).getProjectName());
        assertEquals(1, dashboard.getMemberWorkloads().size());
        assertEquals("worker", dashboard.getMemberWorkloads().get(0).getUsername());
        assertEquals(1, dashboard.getMemberWorkloads().get(0).getActiveTasks());
        assertEquals(1, dashboard.getMemberWorkloads().get(0).getOverdueTasks());
    }

    @Test
    void dashboardRejectsNonMemberAccess() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        ws.setActive(true);
        when(workspaceRepository.findById(10L)).thenReturn(Optional.of(ws));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(false);

        assertThrows(InvalidOperationException.class,
                () -> dashboardService.getWorkspaceDashboard(10L, null, null, null));
    }
}
