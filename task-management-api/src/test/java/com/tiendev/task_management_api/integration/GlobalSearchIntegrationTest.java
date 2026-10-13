package com.tiendev.task_management_api.integration;

import com.tiendev.task_management_api.dto.GlobalSearchResponse;
import com.tiendev.task_management_api.model.*;
import com.tiendev.task_management_api.model.enums.*;
import com.tiendev.task_management_api.repository.*;
import com.tiendev.task_management_api.service.SearchService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class GlobalSearchIntegrationTest {

    @Autowired private SearchService searchService;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TagRepository tagRepository;

    private User user1;
    private User user2;
    private Workspace wsA;
    private Workspace wsB;
    private Project projA;
    private Project projB;

    @BeforeEach
    void setUp() {
        user1 = new User();
        user1.setUsername("search_user1");
        user1.setEmail("search1@example.com");
        user1.setPassword("password123");
        user1.setRole(Role.USER);
        user1 = userRepository.save(user1);

        user2 = new User();
        user2.setUsername("search_user2");
        user2.setEmail("search2@example.com");
        user2.setPassword("password123");
        user2.setRole(Role.USER);
        user2 = userRepository.save(user2);

        // Workspace A - user1 is member
        wsA = new Workspace();
        wsA.setName("Fintech Alpha Workspace");
        wsA.setDescription("Core financial platform");
        wsA.setActive(true);
        wsA = workspaceRepository.save(wsA);

        WorkspaceMember memberA = new WorkspaceMember();
        memberA.setWorkspace(wsA);
        memberA.setUser(user1);
        memberA.setRole(WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(memberA);

        projA = new Project();
        projA.setName("Billing Alpha Module");
        projA.setDescription("Handles payment processing");
        projA.setWorkspace(wsA);
        projA.setStatus(ProjectStatus.ACTIVE);
        projA.setActive(true);
        projA = projectRepository.save(projA);

        // Workspace B - user2 is member (user1 has NO access)
        wsB = new Workspace();
        wsB.setName("Secret Alpha Hospital");
        wsB.setDescription("Private medical records");
        wsB.setActive(true);
        wsB = workspaceRepository.save(wsB);

        WorkspaceMember memberB = new WorkspaceMember();
        memberB.setWorkspace(wsB);
        memberB.setUser(user2);
        memberB.setRole(WorkspaceRole.MEMBER);
        workspaceMemberRepository.save(memberB);

        projB = new Project();
        projB.setName("Patient Alpha System");
        projB.setDescription("Medical records project");
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
        tagRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void searchFindsMatchesAndNeverLeaksDataFromOtherWorkspaces() {
        // Tag in Workspace A
        Tag tagA = new Tag();
        tagA.setWorkspace(wsA);
        tagA.setName("alpha-tag");
        tagA = tagRepository.save(tagA);

        // Task A1: matching by tag
        Task tA1 = new Task();
        tA1.setTitle("Checkout Feature");
        tA1.setDescription("Regular description");
        tA1.setProject(projA);
        tA1.setPriority(TaskPriority.HIGH);
        tA1.setStatus(TaskStatus.TODO);
        tA1.setActive(true);
        tA1.setTags(Set.of(tagA));
        taskRepository.save(tA1);

        // Task A2: matching by title
        Task tA2 = new Task();
        tA2.setTitle("Alpha Task in Member Workspace");
        tA2.setDescription("Important task");
        tA2.setProject(projA);
        tA2.setPriority(TaskPriority.MEDIUM);
        tA2.setStatus(TaskStatus.IN_PROGRESS);
        tA2.setActive(true);
        taskRepository.save(tA2);

        // Task B1: in Workspace B (user1 is not member) - contains 'Alpha' in title
        Task tB1 = new Task();
        tB1.setTitle("Alpha Secret Task in Private Workspace");
        tB1.setDescription("Confidential");
        tB1.setProject(projB);
        tB1.setPriority(TaskPriority.HIGH);
        tB1.setStatus(TaskStatus.TODO);
        tB1.setActive(true);
        taskRepository.save(tB1);

        // Search "alpha" (case-insensitive test)
        GlobalSearchResponse res = searchService.search("ALPHA", null);

        // Workspaces: only Workspace A, never Workspace B
        assertEquals(1, res.getWorkspaces().size());
        assertEquals("Fintech Alpha Workspace", res.getWorkspaces().get(0).getName());

        // Projects: only Project A, never Project B
        assertEquals(1, res.getProjects().size());
        assertEquals("Billing Alpha Module", res.getProjects().get(0).getName());

        // Tasks: Task A1 (via tag) and Task A2 (via title), never Task B1
        assertEquals(2, res.getTasks().size());
        assertTrue(res.getTasks().stream().noneMatch(t -> t.getId().equals(tB1.getId())));
        assertTrue(res.getTasks().stream().allMatch(t -> t.getWorkspaceId().equals(wsA.getId())));
    }

    @Test
    void searchWithOptionalWorkspaceIdFiltersCorrectly() {
        Task tA = new Task();
        tA.setTitle("Keyword Search Test");
        tA.setDescription("Contains keyword in Workspace A");
        tA.setProject(projA);
        tA.setPriority(TaskPriority.MEDIUM);
        tA.setStatus(TaskStatus.TODO);
        tA.setActive(true);
        taskRepository.save(tA);

        // Query within accessible Workspace A
        GlobalSearchResponse resA = searchService.search("Keyword", wsA.getId());
        assertEquals(1, resA.getTasks().size());
        assertEquals(tA.getId(), resA.getTasks().get(0).getId());

        // Query specifying Workspace B which user1 has no access to -> returns empty
        GlobalSearchResponse resB = searchService.search("Keyword", wsB.getId());
        assertTrue(resB.getWorkspaces().isEmpty());
        assertTrue(resB.getProjects().isEmpty());
        assertTrue(resB.getTasks().isEmpty());
    }

    @Test
    void searchReturnsEmptyOnBlankQuery() {
        GlobalSearchResponse res = searchService.search("   ", null);
        assertTrue(res.getWorkspaces().isEmpty());
        assertTrue(res.getProjects().isEmpty());
        assertTrue(res.getTasks().isEmpty());
    }
}
