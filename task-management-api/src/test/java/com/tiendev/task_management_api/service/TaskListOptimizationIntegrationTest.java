package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.request.TaskFilterRequest;
import com.tiendev.task_management_api.model.*;
import com.tiendev.task_management_api.model.enums.ProjectStatus;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.*;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TaskListOptimizationIntegrationTest {

    @Autowired private TaskService taskService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TagRepository tagRepository;
    @Autowired private TaskChecklistItemRepository checklistItemRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private EntityManager entityManager;

    private User user;
    private Workspace workspace;
    private Project project;
    private Tag tag;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("task_perf_user");
        user.setEmail("task_perf@example.com");
        user.setPassword("password123");
        user.setRole(Role.USER);
        user = userRepository.save(user);

        workspace = new Workspace();
        workspace.setName("Performance Workspace");
        workspace.setActive(true);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(WorkspaceRole.OWNER);
        member.setJoinedAt(LocalDateTime.now());
        workspaceMemberRepository.save(member);

        project = new Project();
        project.setName("Performance Project");
        project.setWorkspace(workspace);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setActive(true);
        project = projectRepository.save(project);

        tag = new Tag();
        tag.setName("urgent-tag");
        tag.setColor("#FF0000");
        tag.setWorkspace(workspace);
        tag = tagRepository.save(tag);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        commentRepository.deleteAll();
        checklistItemRepository.deleteAll();
        taskRepository.deleteAll();
        tagRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Response comparison: getById preserves full details (tags, checklist, comments) while list view avoids heavy collection fetching")
    void testResponseDetailsConsistency() {
        // Create a parent task
        Task task = new Task();
        task.setTitle("Detailed Master Task");
        task.setDescription("Master task description");
        task.setProject(project);
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(TaskPriority.HIGH);
        task.setAssignee(user);
        task.setDeadline(LocalDate.now().plusDays(5));
        task.setActive(true);
        task.setTags(Set.of(tag));
        task = taskRepository.save(task);

        // Add checklist items
        TaskChecklistItem item1 = new TaskChecklistItem();
        item1.setTask(task);
        item1.setTitle("Checklist Step 1");
        item1.setCompleted(true);
        item1.setPosition(0);
        checklistItemRepository.save(item1);

        TaskChecklistItem item2 = new TaskChecklistItem();
        item2.setTask(task);
        item2.setTitle("Checklist Step 2");
        item2.setCompleted(false);
        item2.setPosition(1);
        checklistItemRepository.save(item2);

        // Add comment
        Comment comment = new Comment();
        comment.setTask(task);
        comment.setUser(user);
        comment.setContent("Task progress comment");
        commentRepository.save(comment);

        // Add 2 subtasks (1 DONE, 1 TODO)
        Task sub1 = new Task();
        sub1.setTitle("Subtask 1");
        sub1.setProject(project);
        sub1.setParentTask(task);
        sub1.setStatus(TaskStatus.DONE);
        sub1.setPriority(TaskPriority.LOW);
        sub1.setActive(true);
        taskRepository.save(sub1);

        Task sub2 = new Task();
        sub2.setTitle("Subtask 2");
        sub2.setProject(project);
        sub2.setParentTask(task);
        sub2.setStatus(TaskStatus.TODO);
        sub2.setPriority(TaskPriority.LOW);
        sub2.setActive(true);
        taskRepository.save(sub2);

        // 1. Verify getById has full details
        TaskResponse detail = taskService.getById(task.getId());
        assertEquals("Detailed Master Task", detail.getTitle());
        assertEquals("Master task description", detail.getDescription());
        assertEquals(user.getId(), detail.getAssignee().getId());
        assertEquals(2, detail.getSubtaskCount());
        assertEquals(1, detail.getCompletedSubtaskCount());
        assertEquals(1, detail.getTags().size());
        assertEquals("urgent-tag", detail.getTags().get(0).getName());
        assertEquals(2, detail.getChecklist().size());
        assertEquals(1, detail.getCommentCount());

        // 2. Verify list view preserves the existing response contract without N+1 queries
        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setProjectId(project.getId());
        PageResponse<TaskResponse> listResponse = taskService.getAll(filter, PageRequest.of(0, 20));

        final Long targetTaskId = task.getId();
        TaskResponse listTask = listResponse.getContent().stream()
                .filter(t -> t.getId().equals(targetTaskId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Task not found in list response"));

        assertEquals("Detailed Master Task", listTask.getTitle());
        assertEquals("Master task description", listTask.getDescription());
        assertEquals(TaskStatus.IN_PROGRESS, listTask.getStatus());
        assertEquals(TaskPriority.HIGH, listTask.getPriority());
        assertEquals(user.getId(), listTask.getAssignee().getId());
        assertEquals(2, listTask.getSubtaskCount());
        assertEquals(1, listTask.getCompletedSubtaskCount());
        assertEquals(1, listTask.getTags().size());
        assertEquals("urgent-tag", listTask.getTags().get(0).getName());
        assertEquals(2, listTask.getChecklist().size());
        assertEquals(1, listTask.getCommentCount());
    }

    @Test
    @DisplayName("N+1 query benchmark: loading 10 tasks executes O(1) SQL queries")
    void testQueryCountElimination() {
        // Create 10 tasks, each with subtasks, checklist, and comments
        for (int i = 0; i < 10; i++) {
            Task task = new Task();
            task.setTitle("Benchmark Task " + i);
            task.setProject(project);
            task.setStatus(TaskStatus.TODO);
            task.setPriority(TaskPriority.MEDIUM);
            task.setAssignee(user);
            task.setActive(true);
            task = taskRepository.save(task);

            TaskChecklistItem item = new TaskChecklistItem();
            item.setTask(task);
            item.setTitle("Item for task " + i);
            item.setCompleted(false);
            item.setPosition(0);
            checklistItemRepository.save(item);

            Comment comment = new Comment();
            comment.setTask(task);
            comment.setUser(user);
            comment.setContent("Comment for task " + i);
            commentRepository.save(comment);

            Task sub = new Task();
            sub.setTitle("Subtask for " + i);
            sub.setProject(project);
            sub.setParentTask(task);
            sub.setStatus(TaskStatus.DONE);
            sub.setPriority(TaskPriority.LOW);
            sub.setActive(true);
            taskRepository.save(sub);
        }

        // Clear persistence context to ensure no entities are cached in first-level cache
        entityManager.clear();

        SessionFactory sessionFactory = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        TaskFilterRequest filter = new TaskFilterRequest();
        filter.setProjectId(project.getId());
        PageResponse<TaskResponse> page = taskService.getAll(filter, PageRequest.of(0, 20));

        long queryCount = stats.getPrepareStatementCount();

        // 10 parent tasks + 10 subtasks = 20 tasks in total returned or filtered
        assertFalse(page.getContent().isEmpty());
        // In unoptimized code with N+1, queryCount would exceed 30 queries (10 tasks * 3-4 collections each).
        assertTrue(queryCount <= 7,
                "Query count should be O(1) and <= 7, but was: " + queryCount);
    }
}
