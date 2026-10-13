package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByWorkspaceIdOrderByNameAsc(Long workspaceId);

    Optional<Tag> findByWorkspaceIdAndName(Long workspaceId, String name);

    boolean existsByWorkspaceIdAndName(Long workspaceId, String name);
}
