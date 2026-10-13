package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.TaskDependencyResponse;
import com.tiendev.task_management_api.dto.request.TaskDependencyCreateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.TaskDependency;
import com.tiendev.task_management_api.model.enums.DependencyType;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.repository.TaskDependencyRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.service.TaskDependencyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TaskDependencyServiceImpl implements TaskDependencyService {

    private final TaskDependencyRepository dependencyRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional
    public TaskDependencyResponse addDependency(Long taskId, TaskDependencyCreateRequest request) {
        if (taskId.equals(request.getPrerequisiteTaskId())) {
            throw new InvalidOperationException("Task cannot depend on itself");
        }

        Task task = getTaskAndValidateAccess(taskId);
        Task otherTask = taskRepository.findById(request.getPrerequisiteTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Target task not found with id: " + request.getPrerequisiteTaskId()));

        if (!task.getProject().getWorkspace().getId().equals(otherTask.getProject().getWorkspace().getId())) {
            throw new InvalidOperationException("Both tasks must belong to the same workspace");
        }

        DependencyType type = request.getType() != null ? request.getType() : DependencyType.DEPENDS_ON;

        // Check if dependency already exists
        if (dependencyRepository.existsByTaskIdAndPrerequisiteTaskId(taskId, otherTask.getId())) {
            throw new ResourceAlreadyExistsException("This dependency already exists between the tasks");
        }

        // Determine prerequisite and dependent for cycle check
        Long prerequisiteId;
        Long dependentId;
        if (type == DependencyType.DEPENDS_ON) {
            dependentId = taskId;
            prerequisiteId = otherTask.getId();
        } else {
            prerequisiteId = taskId;
            dependentId = otherTask.getId();
        }

        // Check cycle: is dependentId already a prerequisite of prerequisiteId?
        if (isReachablePrerequisite(dependentId, prerequisiteId)) {
            throw new InvalidOperationException("Cyclic dependency detected: creating this dependency would cause a cycle");
        }

        TaskDependency dependency = new TaskDependency();
        dependency.setTask(task);
        dependency.setPrerequisiteTask(otherTask);
        dependency.setType(type);

        dependency = dependencyRepository.save(dependency);
        return toResponse(dependency);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskDependencyResponse> getDependencies(Long taskId) {
        getTaskAndValidateAccess(taskId);
        List<TaskDependency> list = dependencyRepository.findByTaskId(taskId);
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void deleteDependency(Long taskId, Long dependencyId) {
        getTaskAndValidateAccess(taskId);
        TaskDependency dep = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Dependency not found with id: " + dependencyId));

        if (!dep.getTask().getId().equals(taskId) && !dep.getPrerequisiteTask().getId().equals(taskId)) {
            throw new InvalidOperationException("Dependency does not belong to this task");
        }

        dependencyRepository.delete(dep);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateCanTransitionToDone(Long taskId) {
        // Find all prerequisites that must be completed before taskId
        // 1. Where task = taskId AND type = DEPENDS_ON -> prerequisiteTask is prerequisite
        List<TaskDependency> directDeps = dependencyRepository.findByTaskIdAndType(taskId, DependencyType.DEPENDS_ON);
        for (TaskDependency dep : directDeps) {
            Task prereq = dep.getPrerequisiteTask();
            if (prereq.getStatus() != TaskStatus.DONE) {
                throw new InvalidOperationException("Cannot complete task because prerequisite task '" + prereq.getTitle() + "' is not completed.");
            }
        }

        // 2. Where prerequisiteTask = taskId AND type = BLOCKS -> task is prerequisite
        List<TaskDependency> blockingDeps = dependencyRepository.findByPrerequisiteTaskId(taskId).stream()
                .filter(d -> d.getType() == DependencyType.BLOCKS)
                .toList();
        for (TaskDependency dep : blockingDeps) {
            Task prereq = dep.getTask();
            if (prereq.getStatus() != TaskStatus.DONE) {
                throw new InvalidOperationException("Cannot complete task because blocker task '" + prereq.getTitle() + "' is not completed.");
            }
        }
    }

    private boolean isReachablePrerequisite(Long targetPrerequisiteId, Long startTaskId) {
        // BFS: Traverse prerequisites of startTaskId. If targetPrerequisiteId is found, return true.
        Set<Long> visited = new HashSet<>();
        Queue<Long> queue = new ArrayDeque<>();
        queue.add(startTaskId);
        visited.add(startTaskId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(targetPrerequisiteId)) {
                return true;
            }

            // Find all prerequisites of current
            List<TaskDependency> direct = dependencyRepository.findByTaskIdAndType(current, DependencyType.DEPENDS_ON);
            for (TaskDependency d : direct) {
                Long prereqId = d.getPrerequisiteTask().getId();
                if (visited.add(prereqId)) {
                    queue.add(prereqId);
                }
            }

            List<TaskDependency> blocking = dependencyRepository.findByPrerequisiteTaskId(current).stream()
                    .filter(d -> d.getType() == DependencyType.BLOCKS)
                    .toList();
            for (TaskDependency d : blocking) {
                Long prereqId = d.getTask().getId();
                if (visited.add(prereqId)) {
                    queue.add(prereqId);
                }
            }
        }

        return false;
    }

    private Task getTaskAndValidateAccess(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        Long currentUserId = getCurrentUserId();
        Long workspaceId = task.getProject().getWorkspace().getId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserId)) {
            throw new InvalidOperationException("You do not have access to this workspace");
        }
        return task;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private TaskDependencyResponse toResponse(TaskDependency dep) {
        return TaskDependencyResponse.builder()
                .id(dep.getId())
                .taskId(dep.getTask().getId())
                .taskTitle(dep.getTask().getTitle())
                .prerequisiteTaskId(dep.getPrerequisiteTask().getId())
                .prerequisiteTaskTitle(dep.getPrerequisiteTask().getTitle())
                .prerequisiteTaskStatus(dep.getPrerequisiteTask().getStatus())
                .type(dep.getType())
                .createdAt(dep.getCreatedAt())
                .build();
    }
}
