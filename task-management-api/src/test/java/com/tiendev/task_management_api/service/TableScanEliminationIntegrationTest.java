package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.CommentResponse;
import com.tiendev.task_management_api.dto.WorkspaceMemberResponse;
import com.tiendev.task_management_api.dto.WorkspaceResponse;
import com.tiendev.task_management_api.model.*;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class TableScanEliminationIntegrationTest {

    @Autowired private WorkspaceService workspaceService;
    @Autowired private WorkspaceMemberService workspaceMemberService;
    @Autowired private CommentService commentService;

    @Autowired private UserRepository userRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private TaskRepository taskRepository;
    @Autowired private CommentRepository commentRepository;

    private User user1;
    private User user2;
    private User user3;
    private Workspace activeWsA;
    private Workspace inactiveWsB;
    private Workspace otherWsC;
    private Project activeProjectA;
    private Task activeTaskA;
    private Task inactiveTaskA;
    private Comment commentActiveTaskA;
    private Comment commentInactiveTaskA;
    private Comment commentWsC;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUsername("filter_user1");
        user1.setEmail("filter1@example.com");
        user1.setPassword("password123");
        user1.setRole(Role.USER);
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("filter_user2");
        user2.setEmail("filter2@example.com");
        user2.setPassword("password123");
        user2.setRole(Role.USER);
        user2 = userRepository.save(user2);

        user3 = new User();
        user3.setUsername("filter_user3");
        user3.setEmail("filter3@example.com");
        user3.setPassword("password123");
        user3.setRole(Role.USER);
        user3 = userRepository.save(user3);

        // Workspace A: Active, user1 is member
        activeWsA = new Workspace();
        activeWsA.setName("Active Workspace A");
        activeWsA.setActive(true);
        activeWsA = workspaceRepository.save(activeWsA);

        WorkspaceMember memA1 = new WorkspaceMember();
        memA1.setWorkspace(activeWsA);
        memA1.setUser(user1);
        memA1.setRole(WorkspaceRole.MEMBER);
        memA1.setJoinedAt(LocalDateTime.now());
        workspaceMemberRepository.save(memA1);

        // Workspace B: Inactive, user1 is member
        inactiveWsB = new Workspace();
        inactiveWsB.setName("Inactive Workspace B");
        inactiveWsB.setActive(false);
        inactiveWsB = workspaceRepository.save(inactiveWsB);

        WorkspaceMember memB1 = new WorkspaceMember();
        memB1.setWorkspace(inactiveWsB);
        memB1.setUser(user1);
        memB1.setRole(WorkspaceRole.MEMBER);
        memB1.setJoinedAt(LocalDateTime.now());
        workspaceMemberRepository.save(memB1);

        // Workspace C: Active, only user2 is member (user1 is NOT member)
        otherWsC = new Workspace();
        otherWsC.setName("Other Workspace C");
        otherWsC.setActive(true);
        otherWsC = workspaceRepository.save(otherWsC);

        WorkspaceMember memC2 = new WorkspaceMember();
        memC2.setWorkspace(otherWsC);
        memC2.setUser(user2);
        memC2.setRole(WorkspaceRole.MEMBER);
        memC2.setJoinedAt(LocalDateTime.now());
        workspaceMemberRepository.save(memC2);

        // Project in Workspace A
        activeProjectA = new Project();
        activeProjectA.setName("Project A");
        activeProjectA.setWorkspace(activeWsA);
        activeProjectA.setStatus(com.tiendev.task_management_api.model.enums.ProjectStatus.ACTIVE);
        activeProjectA.setActive(true);
        activeProjectA = projectRepository.save(activeProjectA);

        // Tasks in Project A
        activeTaskA = new Task();
        activeTaskA.setTitle("Active Task A");
        activeTaskA.setProject(activeProjectA);
        activeTaskA.setStatus(TaskStatus.TODO);
        activeTaskA.setPriority(TaskPriority.MEDIUM);
        activeTaskA.setActive(true);
        activeTaskA = taskRepository.save(activeTaskA);

        inactiveTaskA = new Task();
        inactiveTaskA.setTitle("Inactive Task A");
        inactiveTaskA.setProject(activeProjectA);
        inactiveTaskA.setStatus(TaskStatus.TODO);
        inactiveTaskA.setPriority(TaskPriority.MEDIUM);
        inactiveTaskA.setActive(false);
        inactiveTaskA = taskRepository.save(inactiveTaskA);

        // Comments in Workspace A
        commentActiveTaskA = new Comment();
        commentActiveTaskA.setContent("Comment on active task");
        commentActiveTaskA.setTask(activeTaskA);
        commentActiveTaskA.setUser(user1);
        commentActiveTaskA = commentRepository.save(commentActiveTaskA);

        commentInactiveTaskA = new Comment();
        commentInactiveTaskA.setContent("Comment on inactive task");
        commentInactiveTaskA.setTask(inactiveTaskA);
        commentInactiveTaskA.setUser(user1);
        commentInactiveTaskA = commentRepository.save(commentInactiveTaskA);

        // Project and Task and Comment in Workspace C
        Project projectC = new Project();
        projectC.setName("Project C");
        projectC.setWorkspace(otherWsC);
        projectC.setStatus(com.tiendev.task_management_api.model.enums.ProjectStatus.ACTIVE);
        projectC.setActive(true);
        projectC = projectRepository.save(projectC);

        Task taskC = new Task();
        taskC.setTitle("Task C");
        taskC.setProject(projectC);
        taskC.setStatus(TaskStatus.TODO);
        taskC.setPriority(TaskPriority.LOW);
        taskC.setActive(true);
        taskC = taskRepository.save(taskC);

        commentWsC = new Comment();
        commentWsC.setContent("Comment in WsC");
        commentWsC.setTask(taskC);
        commentWsC.setUser(user2);
        commentWsC = commentRepository.save(commentWsC);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        commentRepository.deleteAll();
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    private void authenticateAs(User user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getId().toString(), null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("WorkspaceService.getAll: returns only active workspaces the user belongs to")
    void testWorkspaceGetAllFiltering() {
        authenticateAs(user1);
        List<WorkspaceResponse> responses = workspaceService.getAll();
        assertEquals(1, responses.size());
        assertEquals(activeWsA.getId(), responses.get(0).getId());

        // User3 is not in any workspace
        authenticateAs(user3);
        List<WorkspaceResponse> user3Responses = workspaceService.getAll();
        assertTrue(user3Responses.isEmpty(), "User not in any workspace must get empty list");
    }

    @Test
    @DisplayName("WorkspaceMemberService.getAll: returns only members of active workspaces the user belongs to")
    void testWorkspaceMemberGetAllFiltering() {
        authenticateAs(user1);
        List<WorkspaceMemberResponse> responses = workspaceMemberService.getAll();
        assertEquals(1, responses.size());
        assertEquals(activeWsA.getId(), responses.get(0).getWorkspaceId());
        assertEquals(user1.getId(), responses.get(0).getUser().getId());

        // User3 is not in any workspace
        authenticateAs(user3);
        List<WorkspaceMemberResponse> user3Responses = workspaceMemberService.getAll();
        assertTrue(user3Responses.isEmpty(), "User not in any workspace must get empty list");
    }

    @Test
    @DisplayName("CommentService.getAll: excludes inactive tasks/workspaces and comments outside user's workspace")
    void testCommentGetAllFiltering() {
        authenticateAs(user1);
        List<CommentResponse> responses = commentService.getAll();
        assertEquals(1, responses.size(), "Should only see comment on active task in active workspace A");
        assertEquals(commentActiveTaskA.getId(), responses.get(0).getId());

        // User3 is not in any workspace
        authenticateAs(user3);
        List<CommentResponse> user3Responses = commentService.getAll();
        assertTrue(user3Responses.isEmpty(), "User not in any workspace must get empty list");
    }
}
