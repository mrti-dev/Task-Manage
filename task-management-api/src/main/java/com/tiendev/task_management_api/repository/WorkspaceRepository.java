package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Workspace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Workspace w WHERE w.id = :id")
    java.util.Optional<Workspace> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT w FROM Workspace w " +
           "WHERE w.active = true " +
           "AND w.id IN :workspaceIds " +
           "AND (LOWER(w.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "  OR LOWER(w.description) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY w.name ASC")
    List<Workspace> searchWorkspaces(
            @Param("workspaceIds") Collection<Long> workspaceIds,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT DISTINCT w FROM Workspace w JOIN w.members m " +
           "WHERE m.user.id = :userId AND w.active = true " +
           "ORDER BY w.id ASC")
    List<Workspace> findActiveWorkspacesByUserId(@Param("userId") Long userId);
}
