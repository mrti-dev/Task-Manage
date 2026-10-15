package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);
    List<WorkspaceMember> findByUserId(Long userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    List<WorkspaceMember> findByWorkspaceIdAndRole(Long workspaceId, WorkspaceRole role);
    long countByWorkspaceIdAndRole(Long workspaceId, WorkspaceRole role);
    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    boolean existsByWorkspaceIdAndUserIdAndRole(Long workspaceId, Long userId, WorkspaceRole role);
    boolean existsByWorkspaceIdAndUserIdAndRoleIn(Long workspaceId, Long userId, java.util.Collection<WorkspaceRole> roles);

    @Query("SELECT wm FROM WorkspaceMember wm " +
           "WHERE wm.workspace.active = true " +
           "AND wm.workspace.id IN (" +
           "   SELECT m.workspace.id FROM WorkspaceMember m " +
           "   WHERE m.user.id = :userId AND m.workspace.active = true" +
           ") " +
           "ORDER BY wm.id ASC")
    List<WorkspaceMember> findActiveWorkspaceMembersByUserId(@Param("userId") Long userId);
}
