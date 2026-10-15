package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    List<Task> findByProjectId(Long projectId);
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findByTags_Id(Long tagId);

    @Query("SELECT t FROM Task t JOIN FETCH t.project p JOIN FETCH p.workspace w JOIN FETCH t.assignee " +
            "WHERE t.active = true AND p.active = true AND w.active = true " +
            "AND t.status <> :completedStatus AND t.deadline <= :latestDeadline")
    List<Task> findActiveTasksForDeadlineReminder(
            @Param("latestDeadline") java.time.LocalDate latestDeadline,
            @Param("completedStatus") TaskStatus completedStatus);

    @Query(value = "SELECT t FROM Task t " +
                   "JOIN FETCH t.project p " +
                   "JOIN FETCH p.workspace w " +
                   "LEFT JOIN FETCH t.assignee a " +
                   "WHERE t.active = true AND p.active = true AND w.active = true AND w.id IN :workspaceIds",
           countQuery = "SELECT count(t) FROM Task t " +
                        "WHERE t.active = true AND t.project.active = true AND t.project.workspace.active = true AND t.project.workspace.id IN :workspaceIds")
    Page<Task> findAllActiveByWorkspaceIds(@Param("workspaceIds") Set<Long> workspaceIds, Pageable pageable);

    @Query("SELECT s.parentTask.id, COUNT(s.id), " +
           "SUM(CASE WHEN s.status = com.tiendev.task_management_api.model.enums.TaskStatus.DONE THEN 1 ELSE 0 END) " +
           "FROM Task s WHERE s.parentTask.id IN :taskIds AND s.active = true GROUP BY s.parentTask.id")
    List<Object[]> countSubtasksByParentTaskIds(@Param("taskIds") java.util.Collection<Long> taskIds);

    @Query("SELECT t.id, tag.id, tag.workspace.id, tag.name, tag.color " +
           "FROM Task t JOIN t.tags tag WHERE t.id IN :taskIds ORDER BY t.id, tag.id")
    List<Object[]> findTagRowsByTaskIds(@Param("taskIds") java.util.Collection<Long> taskIds);

    @Query("SELECT item.task.id, item.id, item.title, item.completed, item.position, item.createdAt " +
           "FROM TaskChecklistItem item WHERE item.task.id IN :taskIds ORDER BY item.task.id, item.position")
    List<Object[]> findChecklistRowsByTaskIds(@Param("taskIds") java.util.Collection<Long> taskIds);

    @Query("SELECT c.task.id, COUNT(c.id) FROM Comment c WHERE c.task.id IN :taskIds GROUP BY c.task.id")
    List<Object[]> countCommentsByTaskIds(@Param("taskIds") java.util.Collection<Long> taskIds);

    @Query("SELECT t FROM Task t JOIN FETCH t.project p LEFT JOIN FETCH t.assignee a WHERE t.active = true AND p.active = true AND p.workspace.id = :workspaceId AND (:projectId IS NULL OR p.id = :projectId)")
    List<Task> findActiveTasksForDashboard(@Param("workspaceId") Long workspaceId, @Param("projectId") Long projectId);

    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.project p " +
           "JOIN FETCH p.workspace w " +
           "LEFT JOIN FETCH t.assignee a " +
           "WHERE t.active = true " +
           "AND p.active = true " +
           "AND w.active = true " +
           "AND w.id IN :workspaceIds " +
           "AND t.deadline BETWEEN :from AND :to " +
           "AND (:projectId IS NULL OR p.id = :projectId) " +
           "AND (:assigneeId IS NULL OR (a IS NOT NULL AND a.id = :assigneeId)) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:status IS NULL OR t.status = :status) " +
           "ORDER BY t.deadline ASC, t.id ASC")
    List<Task> findCalendarTasks(
            @Param("workspaceIds") java.util.Collection<Long> workspaceIds,
            @Param("from") java.time.LocalDate from,
            @Param("to") java.time.LocalDate to,
            @Param("projectId") Long projectId,
            @Param("assigneeId") Long assigneeId,
            @Param("priority") com.tiendev.task_management_api.model.enums.TaskPriority priority,
            @Param("status") com.tiendev.task_management_api.model.enums.TaskStatus status);

    @Query("SELECT DISTINCT t FROM Task t " +
           "JOIN FETCH t.project p " +
           "JOIN FETCH p.workspace w " +
           "LEFT JOIN t.tags tag " +
           "WHERE t.active = true " +
           "AND p.active = true " +
           "AND w.active = true " +
           "AND w.id IN :workspaceIds " +
           "AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "  OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "  OR LOWER(tag.name) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.id DESC")
    List<Task> searchTasks(
            @Param("workspaceIds") java.util.Collection<Long> workspaceIds,
            @Param("query") String query,
            Pageable pageable);
}
