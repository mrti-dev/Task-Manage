package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.WorkspaceDashboardResponse;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.enums.TaskPriority;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import com.tiendev.task_management_api.repository.ProjectRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDashboardResponse getWorkspaceDashboard(Long workspaceId, Long projectId, LocalDate from, LocalDate to) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));
        if (!workspace.isActive()) {
            throw new ResourceNotFoundException("Workspace not found with id: " + workspaceId);
        }

        Long currentUserId = getCurrentUserId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserId)) {
            throw new InvalidOperationException("You do not have access to this workspace");
        }

        List<Task> tasks = taskRepository.findActiveTasksForDashboard(workspaceId, projectId);

        LocalDate today = LocalDate.now();

        long totalTasks = tasks.size();
        long completedTasks = tasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
        double completionRate = totalTasks > 0 ? Math.round(((double) completedTasks / totalTasks) * 1000.0) / 10.0 : 0.0;

        long overdueTasks = tasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(today) && t.getStatus() != TaskStatus.DONE)
                .count();

        long dueSoonTasks = tasks.stream()
                .filter(t -> t.getDeadline() != null && !t.getDeadline().isBefore(today)
                        && !t.getDeadline().isAfter(today.plusDays(2)) && t.getStatus() != TaskStatus.DONE)
                .count();

        Map<String, Long> tasksByStatus = new LinkedHashMap<>();
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status.name(), tasks.stream().filter(t -> t.getStatus() == status).count());
        }

        Map<String, Long> tasksByPriority = new LinkedHashMap<>();
        for (TaskPriority priority : TaskPriority.values()) {
            tasksByPriority.put(priority.name(), tasks.stream().filter(t -> t.getPriority() == priority).count());
        }

        // Project progress
        List<Project> activeProjects = projectRepository.findByWorkspaceIdAndActiveTrue(workspaceId).stream()
                .filter(p -> projectId == null || p.getId().equals(projectId))
                .toList();

        Map<Long, List<Task>> tasksByProject = tasks.stream().collect(Collectors.groupingBy(t -> t.getProject().getId()));

        List<WorkspaceDashboardResponse.ProjectProgressItem> projectProgress = activeProjects.stream().map(p -> {
            List<Task> pTasks = tasksByProject.getOrDefault(p.getId(), Collections.emptyList());
            long pTotal = pTasks.size();
            long pCompleted = pTasks.stream().filter(t -> t.getStatus() == TaskStatus.DONE).count();
            double pRate = pTotal > 0 ? Math.round(((double) pCompleted / pTotal) * 1000.0) / 10.0 : 0.0;
            long pOverdue = pTasks.stream()
                    .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(today) && t.getStatus() != TaskStatus.DONE)
                    .count();

            return WorkspaceDashboardResponse.ProjectProgressItem.builder()
                    .projectId(p.getId())
                    .projectName(p.getName())
                    .totalTasks(pTotal)
                    .completedTasks(pCompleted)
                    .completionRate(pRate)
                    .overdueTasks(pOverdue)
                    .build();
        }).toList();

        // Member workloads
        Map<User, List<Task>> tasksByUser = tasks.stream()
                .filter(t -> t.getAssignee() != null)
                .collect(Collectors.groupingBy(Task::getAssignee));

        List<WorkspaceDashboardResponse.MemberWorkloadItem> memberWorkloads = tasksByUser.entrySet().stream().map(entry -> {
            User user = entry.getKey();
            List<Task> uTasks = entry.getValue();

            long activeCount = uTasks.stream().filter(t -> t.getStatus() != TaskStatus.DONE).count();
            long overdueCount = uTasks.stream()
                    .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(today) && t.getStatus() != TaskStatus.DONE)
                    .count();

            long completedInRange = uTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .filter(t -> {
                        if (from == null && to == null) return true;
                        LocalDate taskDate = t.getUpdatedAt() != null ? t.getUpdatedAt().toLocalDate() : t.getCreatedAt().toLocalDate();
                        boolean afterFrom = from == null || !taskDate.isBefore(from);
                        boolean beforeTo = to == null || !taskDate.isAfter(to);
                        return afterFrom && beforeTo;
                    })
                    .count();

            Map<String, Long> userPriorityMap = new LinkedHashMap<>();
            for (TaskPriority priority : TaskPriority.values()) {
                userPriorityMap.put(priority.name(), uTasks.stream().filter(t -> t.getPriority() == priority && t.getStatus() != TaskStatus.DONE).count());
            }

            return WorkspaceDashboardResponse.MemberWorkloadItem.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .avatar(user.getAvatar())
                    .activeTasks(activeCount)
                    .overdueTasks(overdueCount)
                    .completedInRange(completedInRange)
                    .tasksByPriority(userPriorityMap)
                    .build();
        }).sorted(Comparator.comparingLong(WorkspaceDashboardResponse.MemberWorkloadItem::getActiveTasks).reversed())
          .toList();

        // Overdue trends by date
        Map<LocalDate, Long> overdueByDate = tasks.stream()
                .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(today) && t.getStatus() != TaskStatus.DONE)
                .collect(Collectors.groupingBy(Task::getDeadline, Collectors.counting()));

        List<WorkspaceDashboardResponse.OverdueTrendItem> overdueTrends = overdueByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> WorkspaceDashboardResponse.OverdueTrendItem.builder()
                        .date(e.getKey().toString())
                        .count(e.getValue())
                        .build())
                .toList();

        return WorkspaceDashboardResponse.builder()
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .completionRate(completionRate)
                .overdueTasks(overdueTasks)
                .dueSoonTasks(dueSoonTasks)
                .tasksByStatus(tasksByStatus)
                .tasksByPriority(tasksByPriority)
                .projectProgress(projectProgress)
                .memberWorkloads(memberWorkloads)
                .overdueTrends(overdueTrends)
                .build();
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }
}
