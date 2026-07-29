package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Task;
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

    @Query("SELECT t FROM Task t WHERE t.active = true AND t.project.active = true AND t.project.workspace.active = true AND t.project.workspace.id IN :workspaceIds")
    Page<Task> findAllActiveByWorkspaceIds(@Param("workspaceIds") Set<Long> workspaceIds, Pageable pageable);
}
