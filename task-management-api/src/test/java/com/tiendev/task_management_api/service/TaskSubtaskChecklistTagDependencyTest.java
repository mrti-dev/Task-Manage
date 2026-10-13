package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.request.ChecklistItemCreateRequest;
import com.tiendev.task_management_api.dto.request.ChecklistReorderRequest;
import com.tiendev.task_management_api.dto.request.TaskCreateRequest;
import com.tiendev.task_management_api.dto.request.TaskDependencyCreateRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.TaskChecklistItem;
import com.tiendev.task_management_api.model.TaskDependency;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.enums.DependencyType;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.*;
import com.tiendev.task_management_api.service.impl.TaskChecklistServiceImpl;
import com.tiendev.task_management_api.service.impl.TaskDependencyServiceImpl;
import com.tiendev.task_management_api.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskSubtaskChecklistTagDependencyTest {

    @Mock private TaskRepository taskRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private UserRepository userRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private TaskChecklistItemRepository checklistItemRepository;
    @Mock private TaskDependencyRepository dependencyRepository;
    @Mock private TagRepository tagRepository;
    @Mock private NotificationService notificationService;
    @Mock private ActivityLogService activityLogService;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private TaskDependencyService taskDependencyService;

    @InjectMocks private TaskServiceImpl taskService;
    @InjectMocks private TaskChecklistServiceImpl checklistService;
    @InjectMocks private TaskDependencyServiceImpl dependencyServiceImpl;

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
    void rejectsNestedSubtasksBeyondOneLevel() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        ws.setActive(true);
        Project project = new Project();
        project.setId(20L);
        project.setWorkspace(ws);
        project.setActive(true);

        Task grandParent = new Task();
        grandParent.setId(100L);

        Task parent = new Task();
        parent.setId(101L);
        parent.setParentTask(grandParent); // already a subtask

        when(projectRepository.findById(20L)).thenReturn(Optional.of(project));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserIdAndRole(10L, 1L, WorkspaceRole.OWNER)).thenReturn(true);
        when(taskRepository.findById(101L)).thenReturn(Optional.of(parent));

        TaskCreateRequest req = TaskCreateRequest.builder()
                .projectId(20L)
                .title("Sub-subtask")
                .priority(TaskPriority.MEDIUM)
                .parentId(101L)
                .build();

        assertThrows(InvalidOperationException.class, () -> taskService.create(req));
    }

    @Test
    void checklistAddRequiresWorkspaceMembership() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        Project prj = new Project();
        prj.setWorkspace(ws);
        Task task = new Task();
        task.setId(50L);
        task.setProject(prj);

        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(false);

        ChecklistItemCreateRequest req = new ChecklistItemCreateRequest("Item 1");
        assertThrows(InvalidOperationException.class, () -> checklistService.addChecklistItem(50L, req));
    }

    @Test
    void checklistUpdateBroadcastsSnapshotToWorkspaceScopedTopic() {
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        Project project = new Project();
        project.setWorkspace(workspace);
        Task task = new Task();
        task.setId(50L);
        task.setProject(project);
        TaskChecklistItem item = new TaskChecklistItem();
        item.setId(1L);
        item.setTask(task);

        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
        when(checklistItemRepository.findByTaskIdOrderByPositionAsc(50L)).thenReturn(List.of(), List.of(item));
        when(checklistItemRepository.save(any(TaskChecklistItem.class))).thenReturn(item);

        checklistService.addChecklistItem(50L, new ChecklistItemCreateRequest("Item"));

        verify(messagingTemplate).convertAndSend(
                eq("/topic/workspace/10/tasks/50/checklist"), anyList());
    }

    @Test
    void rejectsChecklistOrderWithDuplicateOrMissingItems() {
        Workspace workspace = new Workspace();
        workspace.setId(10L);
        Project project = new Project();
        project.setWorkspace(workspace);
        Task task = new Task();
        task.setId(50L);
        task.setProject(project);

        TaskChecklistItem first = new TaskChecklistItem();
        first.setId(1L);
        first.setTask(task);
        TaskChecklistItem second = new TaskChecklistItem();
        second.setId(2L);
        second.setTask(task);

        when(taskRepository.findById(50L)).thenReturn(Optional.of(task));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);
        when(checklistItemRepository.findByTaskIdOrderByPositionAsc(50L)).thenReturn(List.of(first, second));

        assertThrows(BusinessException.class,
                () -> checklistService.reorderChecklist(50L, new ChecklistReorderRequest(List.of(1L, 1L))));
        assertThrows(BusinessException.class,
                () -> checklistService.reorderChecklist(50L, new ChecklistReorderRequest(List.of(1L))));
    }

    @Test
    void rejectsSelfDependency() {
        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(50L, DependencyType.DEPENDS_ON);
        assertThrows(InvalidOperationException.class, () -> dependencyServiceImpl.addDependency(50L, req));
    }

    @Test
    void rejectsCyclicDependency() {
        Workspace ws = new Workspace();
        ws.setId(10L);
        Project prj = new Project();
        prj.setWorkspace(ws);

        Task taskA = new Task();
        taskA.setId(1L);
        taskA.setProject(prj);

        Task taskB = new Task();
        taskB.setId(2L);
        taskB.setProject(prj);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(taskA));
        when(taskRepository.findById(2L)).thenReturn(Optional.of(taskB));
        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 1L)).thenReturn(true);

        // Suppose taskB already DEPENDS_ON taskA
        TaskDependency existingDep = new TaskDependency();
        existingDep.setTask(taskB);
        existingDep.setPrerequisiteTask(taskA);
        existingDep.setType(DependencyType.DEPENDS_ON);
        when(dependencyRepository.findByTaskIdAndType(2L, DependencyType.DEPENDS_ON)).thenReturn(List.of(existingDep));

        // Now trying to make taskA DEPEND_ON taskB would create cycle A -> B -> A
        TaskDependencyCreateRequest req = new TaskDependencyCreateRequest(2L, DependencyType.DEPENDS_ON);
        assertThrows(InvalidOperationException.class, () -> dependencyServiceImpl.addDependency(1L, req));
    }

    @Test
    void blocksTransitionToDoneIfPrerequisiteNotCompleted() {
        Task prerequisite = new Task();
        prerequisite.setId(2L);
        prerequisite.setTitle("Prerequisite Task");
        prerequisite.setStatus(TaskStatus.IN_PROGRESS);

        TaskDependency dep = new TaskDependency();
        dep.setTask(new Task());
        dep.setPrerequisiteTask(prerequisite);
        dep.setType(DependencyType.DEPENDS_ON);

        when(dependencyRepository.findByTaskIdAndType(1L, DependencyType.DEPENDS_ON)).thenReturn(List.of(dep));

        assertThrows(InvalidOperationException.class, () -> dependencyServiceImpl.validateCanTransitionToDone(1L));
    }

    @Test
    void allowsTransitionToDoneWhenPrerequisiteCompleted() {
        Task prerequisite = new Task();
        prerequisite.setId(2L);
        prerequisite.setTitle("Prerequisite Task");
        prerequisite.setStatus(TaskStatus.DONE);

        TaskDependency dep = new TaskDependency();
        dep.setTask(new Task());
        dep.setPrerequisiteTask(prerequisite);
        dep.setType(DependencyType.DEPENDS_ON);

        when(dependencyRepository.findByTaskIdAndType(1L, DependencyType.DEPENDS_ON)).thenReturn(List.of(dep));

        assertDoesNotThrow(() -> dependencyServiceImpl.validateCanTransitionToDone(1L));
    }
}
