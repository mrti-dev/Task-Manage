package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
