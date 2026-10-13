package com.tiendev.task_management_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkspaceDashboardResponse {

    private long totalTasks;
    private long completedTasks;
    private double completionRate;
    private long overdueTasks;
    private long dueSoonTasks;

    private Map<String, Long> tasksByStatus;
    private Map<String, Long> tasksByPriority;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectProgressItem {
        private Long projectId;
        private String projectName;
        private long totalTasks;
        private long completedTasks;
        private double completionRate;
        private long overdueTasks;
    }
    private List<ProjectProgressItem> projectProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberWorkloadItem {
        private Long userId;
        private String username;
        private String email;
        private String avatar;
        private long activeTasks;
        private long overdueTasks;
        private long completedInRange;
        private Map<String, Long> tasksByPriority;
    }
    private List<MemberWorkloadItem> memberWorkloads;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OverdueTrendItem {
        private String date;
        private long count;
    }
    private List<OverdueTrendItem> overdueTrends;
}
