package com.tiendev.task_management_api.repository.spec;

import com.tiendev.task_management_api.dto.request.ProjectFilterRequest;
import com.tiendev.task_management_api.model.Project;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProjectSpecifications {

    public static Specification<Project> isActive() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("active")),
                cb.isTrue(root.get("workspace").get("active"))
        );
    }

    public static Specification<Project> inWorkspaces(Set<Long> workspaceIds) {
        return (root, query, cb) -> root.get("workspace").get("id").in(workspaceIds);
    }

    public static Specification<Project> fromFilter(ProjectFilterRequest filter) {
        if (filter == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.getWorkspaceId() != null) {
                predicates.add(cb.equal(root.get("workspace").get("id"), filter.getWorkspaceId()));
            }
            if (filter.getName() != null && !filter.getName().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("name")),
                        "%" + filter.getName().toLowerCase() + "%"));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getStartDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDateFrom()));
            }
            if (filter.getStartDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.getStartDateTo()));
            }
            if (filter.getEndDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("endDate"), filter.getEndDateFrom()));
            }
            if (filter.getEndDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endDate"), filter.getEndDateTo()));
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
