package com.tiendev.task_management_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalSearchResponse {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkspaceSearchItem {
        private Long id;
        private String name;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectSearchItem {
        private Long id;
        private String name;
        private String description;
        private Long workspaceId;
        private String workspaceName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskSearchItem {
        private Long id;
        private String title;
        private String description;
        private String status;
        private String priority;
        private Long projectId;
        private String projectName;
        private Long workspaceId;
        private String workspaceName;
    }

    private List<WorkspaceSearchItem> workspaces;
    private List<ProjectSearchItem> projects;
    private List<TaskSearchItem> tasks;
}
