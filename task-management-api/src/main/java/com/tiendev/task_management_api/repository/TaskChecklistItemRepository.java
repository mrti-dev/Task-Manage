package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.TaskChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskChecklistItemRepository extends JpaRepository<TaskChecklistItem, Long> {

    List<TaskChecklistItem> findByTaskIdOrderByPositionAsc(Long taskId);

    long countByTaskIdAndCompletedTrue(Long taskId);

    long countByTaskId(Long taskId);
}
