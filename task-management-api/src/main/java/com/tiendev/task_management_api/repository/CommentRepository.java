package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByTaskId(Long taskId);
    List<Comment> findByUserId(Long userId);

    @Query("SELECT c FROM Comment c " +
           "WHERE c.task.active = true " +
           "AND c.task.project.active = true " +
           "AND c.task.project.workspace.active = true " +
           "AND c.task.project.workspace.id IN (" +
           "   SELECT wm.workspace.id FROM WorkspaceMember wm " +
           "   WHERE wm.user.id = :userId AND wm.workspace.active = true" +
           ") " +
           "ORDER BY c.id ASC")
    List<Comment> findActiveCommentsByUserId(@Param("userId") Long userId);
}
