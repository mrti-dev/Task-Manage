package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.GlobalSearchResponse;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.repository.ProjectRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final WorkspaceRepository workspaceRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public GlobalSearchResponse search(String query, Long workspaceId) {
        if (query == null || query.isBlank()) {
            return GlobalSearchResponse.builder()
                    .workspaces(Collections.emptyList())
                    .projects(Collections.emptyList())
                    .tasks(Collections.emptyList())
                    .build();
        }

        String q = query.trim().toLowerCase();
        Set<Long> allowedWorkspaceIds = getMemberWorkspaceIds();

        if (workspaceId != null) {
            if (!allowedWorkspaceIds.contains(workspaceId)) {
                return GlobalSearchResponse.builder()
                        .workspaces(Collections.emptyList())
                        .projects(Collections.emptyList())
                        .tasks(Collections.emptyList())
                        .build();
            }
            allowedWorkspaceIds = Set.of(workspaceId);
        }

        if (allowedWorkspaceIds.isEmpty()) {
            return GlobalSearchResponse.builder()
                    .workspaces(Collections.emptyList())
                    .projects(Collections.emptyList())
                    .tasks(Collections.emptyList())
                    .build();
        }

        final Set<Long> finalWsIds = allowedWorkspaceIds;

        List<GlobalSearchResponse.WorkspaceSearchItem> workspaces = workspaceRepository.searchWorkspaces(
                finalWsIds, q, org.springframework.data.domain.PageRequest.of(0, 10)).stream()
                .map(w -> GlobalSearchResponse.WorkspaceSearchItem.builder()
                        .id(w.getId())
                        .name(w.getName())
                        .description(w.getDescription())
                        .build())
                .toList();

        List<GlobalSearchResponse.ProjectSearchItem> projects = projectRepository.searchProjects(
                finalWsIds, q, org.springframework.data.domain.PageRequest.of(0, 10)).stream()
                .map(p -> GlobalSearchResponse.ProjectSearchItem.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .description(p.getDescription())
                        .workspaceId(p.getWorkspace().getId())
                        .workspaceName(p.getWorkspace().getName())
                        .build())
                .toList();

        List<GlobalSearchResponse.TaskSearchItem> tasks = taskRepository.searchTasks(
                finalWsIds, q, org.springframework.data.domain.PageRequest.of(0, 15)).stream()
                .map(t -> GlobalSearchResponse.TaskSearchItem.builder()
                        .id(t.getId())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .status(t.getStatus() != null ? t.getStatus().name() : null)
                        .priority(t.getPriority() != null ? t.getPriority().name() : null)
                        .projectId(t.getProject().getId())
                        .projectName(t.getProject().getName())
                        .workspaceId(t.getProject().getWorkspace().getId())
                        .workspaceName(t.getProject().getWorkspace().getName())
                        .build())
                .toList();

        return GlobalSearchResponse.builder()
                .workspaces(workspaces)
                .projects(projects)
                .tasks(tasks)
                .build();
    }

    private Set<Long> getMemberWorkspaceIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Collections.emptySet();
        }
        try {
            Long currentUserId = Long.valueOf(auth.getName());
            return workspaceMemberRepository.findByUserId(currentUserId).stream()
                    .map(m -> m.getWorkspace().getId())
                    .collect(Collectors.toSet());
        } catch (NumberFormatException e) {
            return Collections.emptySet();
        }
    }
}
