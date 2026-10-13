package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.SavedFilter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedFilterRepository extends JpaRepository<SavedFilter, Long> {

    List<SavedFilter> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<SavedFilter> findByUserIdAndWorkspaceIdOrderByCreatedAtDesc(Long userId, Long workspaceId);

    Optional<SavedFilter> findByIdAndUserId(Long id, Long userId);
}
