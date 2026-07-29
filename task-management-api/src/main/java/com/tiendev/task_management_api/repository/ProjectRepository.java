package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface ProjectRepository extends JpaRepository<Project, Long>, JpaSpecificationExecutor<Project> {
    List<Project> findByWorkspaceId(Long workspaceId);

    @Query("SELECT p FROM Project p WHERE p.active = true AND p.workspace.active = true AND p.workspace.id IN :workspaceIds")
    Page<Project> findAllActiveByWorkspaceIds(@Param("workspaceIds") Set<Long> workspaceIds, Pageable pageable);
}
