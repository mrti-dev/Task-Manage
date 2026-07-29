package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {
    List<WorkspaceMember> findByWorkspaceId(Long workspaceId);
    List<WorkspaceMember> findByUserId(Long userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(Long workspaceId, Long userId);
    Optional<WorkspaceMember> findByWorkspaceIdAndRole(Long workspaceId, WorkspaceRole role);
    long countByWorkspaceIdAndRole(Long workspaceId, WorkspaceRole role);
    boolean existsByWorkspaceIdAndUserId(Long workspaceId, Long userId);
}
