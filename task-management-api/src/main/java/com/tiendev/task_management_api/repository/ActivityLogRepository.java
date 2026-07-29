package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.ActivityLog;
import com.tiendev.task_management_api.model.enums.EntityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Set;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    Page<ActivityLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(EntityType entityType, Long entityId, Pageable pageable);
    Page<ActivityLog> findByWorkspaceIdInOrderByCreatedAtDesc(Set<Long> workspaceIds, Pageable pageable);
    Page<ActivityLog> findByWorkspaceIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(Long workspaceId, EntityType entityType, Long entityId, Pageable pageable);
}
