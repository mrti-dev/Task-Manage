package com.tiendev.task_management_api.repository;

import com.tiendev.task_management_api.model.TaskDependency;
import com.tiendev.task_management_api.model.enums.DependencyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependency, Long> {

    List<TaskDependency> findByTaskId(Long taskId);

    List<TaskDependency> findByPrerequisiteTaskId(Long prerequisiteTaskId);

    boolean existsByTaskIdAndPrerequisiteTaskId(Long taskId, Long prerequisiteTaskId);

    void deleteByTaskIdAndPrerequisiteTaskId(Long taskId, Long prerequisiteTaskId);

    List<TaskDependency> findByTaskIdAndType(Long taskId, DependencyType type);
}
