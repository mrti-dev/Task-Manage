package com.tiendev.task_management_api.repository.spec;

import com.tiendev.task_management_api.dto.request.TaskFilterRequest;
import com.tiendev.task_management_api.model.Task;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TaskSpecifications {

    public static Specification<Task> isActive() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.isTrue(root.get("project").get("active")),
                cb.isTrue(root.get("project").get("workspace").get("active"))
        );
    }

    public static Specification<Task> inWorkspaces(Set<Long> workspaceIds) {
        return (root, query, cb) -> root.get("project").get("workspace").get("id").in(workspaceIds);
    }

    public static Specification<Task> fromFilter(TaskFilterRequest filter) {
        if (filter == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getProjectId() != null) {
                predicates.add(cb.equal(root.get("project").get("id"), filter.getProjectId()));
            }
            if (filter.getTitle() != null && !filter.getTitle().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")),
                        "%" + filter.getTitle().toLowerCase() + "%"));
            }
            if (filter.getPriority() != null) {
                predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getAssigneeId() != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), filter.getAssigneeId()));
            }
            if (filter.getDeadlineFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("deadline"), filter.getDeadlineFrom()));
            }
            if (filter.getDeadlineTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("deadline"), filter.getDeadlineTo()));
            }
            if (filter.getCreatedAtFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtFrom()));
            }
            if (filter.getCreatedAtTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.getCreatedAtTo()));
            }

            if (predicates.isEmpty()) {
                return null;
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
