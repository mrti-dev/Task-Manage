package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.TaskDependencyResponse;
import com.tiendev.task_management_api.dto.request.TaskDependencyCreateRequest;

import java.util.List;

public interface TaskDependencyService {

    TaskDependencyResponse addDependency(Long taskId, TaskDependencyCreateRequest request);

    List<TaskDependencyResponse> getDependencies(Long taskId);

    void deleteDependency(Long taskId, Long dependencyId);

    void validateCanTransitionToDone(Long taskId);
}
