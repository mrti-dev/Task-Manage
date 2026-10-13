package com.tiendev.task_management_api.integration;

import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.model.*;
import com.tiendev.task_management_api.model.enums.*;
import com.tiendev.task_management_api.repository.*;
import com.tiendev.task_management_api.service.TaskService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class CalendarApiIntegrationTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private UserRepository userRepository;

    private User user1;
    private User user2;
    private Workspace wsA;
    private Workspace wsB;
    private Project projA;
    private Project projB;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUsername("calendar_user1");
        user1.setEmail("calendar1@example.com");
        user1.setPassword("password123");
        user1.setRole(Role.USER);
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("calendar_user2");
        user2.setEmail("calendar2@example.com");
        user2.setPassword("password123");
        user2.setRole(Role.USER);
        user2 = userRepository.save(user2);

        // Workspace A - user1 is member
        wsA = new Workspace();
        wsA.setName("Workspace A");
        wsA.setActive(true);
        wsA = workspaceRepository.save(wsA);

        WorkspaceMember memberA = new WorkspaceMember();
        memberA.setWorkspace(wsA);
        memberA.setUser(user1);
        memberA.setRole(WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(memberA);

        projA = new Project();
        projA.setName("Project A");
        projA.setWorkspace(wsA);
        projA.setStatus(ProjectStatus.ACTIVE);
        projA.setActive(true);
        projA = projectRepository.save(projA);

        // Workspace B - user2 is member (user1 is NOT member)
        wsB = new Workspace();
        wsB.setName("Workspace B");
        wsB.setActive(true);
        wsB = workspaceRepository.save(wsB);

        WorkspaceMember memberB = new WorkspaceMember();
        memberB.setWorkspace(wsB);
        memberB.setUser(user2);
        memberB.setRole(WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(memberB);

        projB = new Project();
        projB.setName("Project B");
        projB.setWorkspace(wsB);
        projB.setStatus(ProjectStatus.ACTIVE);
        projB.setActive(true);
        projB = projectRepository.save(projB);

        // Set security context to user1
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user1.getId().toString(), null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void getCalendarTasksFiltersByDateRangeAndExcludesOtherWorkspaces() {
        LocalDate baseDate = LocalDate.now();

        // Task 1: in Workspace A, deadline within range
        Task t1 = new Task();
        t1.setTitle("Task A in range");
        t1.setProject(projA);
        t1.setPriority(TaskPriority.HIGH);
        t1.setStatus(TaskStatus.TODO);
        t1.setDeadline(baseDate.plusDays(5));
        t1.setActive(true);
        t1.setAssignee(user1);
        taskRepository.save(t1);

        // Task 2: in Workspace A, deadline OUTSIDE range
        Task t2 = new Task();
        t2.setTitle("Task A out of range");
        t2.setProject(projA);
        t2.setPriority(TaskPriority.MEDIUM);
        t2.setStatus(TaskStatus.IN_PROGRESS);
        t2.setDeadline(baseDate.plusDays(25));
        t2.setActive(true);
        taskRepository.save(t2);

        // Task 3: in Workspace A, inactive
        Task t3 = new Task();
        t3.setTitle("Task A inactive");
        t3.setProject(projA);
        t3.setPriority(TaskPriority.LOW);
        t3.setStatus(TaskStatus.TODO);
        t3.setDeadline(baseDate.plusDays(5));
        t3.setActive(false);
        taskRepository.save(t3);

        // Task 4: in Workspace B (secret), deadline within range
        Task t4 = new Task();
        t4.setTitle("Task B secret");
        t4.setProject(projB);
        t4.setPriority(TaskPriority.HIGH);
        t4.setStatus(TaskStatus.TODO);
        t4.setDeadline(baseDate.plusDays(5));
        t4.setActive(true);
        taskRepository.save(t4);

        LocalDate from = baseDate;
        LocalDate to = baseDate.plusDays(10);

        List<TaskResponse> results = taskService.getCalendarTasks(
                null, null, null, null, null, from, to);

        assertEquals(1, results.size());
        assertEquals("Task A in range", results.get(0).getTitle());
        assertEquals(wsA.getId(), results.get(0).getWorkspaceId());
    }

    @Test
    void getCalendarTasksAppliesAllOptionalFilters() {
        LocalDate baseDate = LocalDate.now();

        Task t1 = new Task();
        t1.setTitle("Match Filter");
        t1.setProject(projA);
        t1.setPriority(TaskPriority.HIGH);
        t1.setStatus(TaskStatus.IN_PROGRESS);
        t1.setDeadline(baseDate.plusDays(3));
        t1.setAssignee(user1);
        t1.setActive(true);
        taskRepository.save(t1);

        Task t2 = new Task();
        t2.setTitle("Different Priority");
        t2.setProject(projA);
        t2.setPriority(TaskPriority.LOW);
        t2.setStatus(TaskStatus.IN_PROGRESS);
        t2.setDeadline(baseDate.plusDays(3));
        t2.setAssignee(user1);
        t2.setActive(true);
        taskRepository.save(t2);

        LocalDate from = baseDate;
        LocalDate to = baseDate.plusDays(10);

        List<TaskResponse> filtered = taskService.getCalendarTasks(
                wsA.getId(), projA.getId(), user1.getId(), TaskPriority.HIGH, TaskStatus.IN_PROGRESS, from, to);

        assertEquals(1, filtered.size());
        assertEquals("Match Filter", filtered.get(0).getTitle());
    }

    @Test
    void getCalendarTasksRejectsInvalidDateRangesAndUnauthorizedWorkspace() {
        LocalDate baseDate = LocalDate.now();

        // from > to
        assertThrows(InvalidOperationException.class, () ->
                taskService.getCalendarTasks(null, null, null, null, null, baseDate.plusDays(5), baseDate));

        // range > 366 days
        assertThrows(InvalidOperationException.class, () ->
                taskService.getCalendarTasks(null, null, null, null, null, baseDate, baseDate.plusDays(400)));

        // null from / to
        assertThrows(InvalidOperationException.class, () ->
                taskService.getCalendarTasks(null, null, null, null, null, null, baseDate));

        // workspace user has no access to
        assertThrows(InvalidOperationException.class, () ->
                taskService.getCalendarTasks(wsB.getId(), null, null, null, null, baseDate, baseDate.plusDays(10)));
    }
}
