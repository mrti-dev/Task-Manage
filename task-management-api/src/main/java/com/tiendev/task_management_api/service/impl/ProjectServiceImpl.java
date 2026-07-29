package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.PageResponse;
import com.tiendev.task_management_api.dto.ProjectResponse;
import com.tiendev.task_management_api.dto.request.ProjectCreateRequest;
import com.tiendev.task_management_api.dto.request.ProjectFilterRequest;
import com.tiendev.task_management_api.dto.request.ProjectUpdateRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.ActivityAction;
import com.tiendev.task_management_api.model.enums.EntityType;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.ProjectStatus;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.ProjectRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.repository.spec.ProjectSpecifications;
import com.tiendev.task_management_api.service.ActivityLogService;
import com.tiendev.task_management_api.service.NotificationService;
import com.tiendev.task_management_api.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        Workspace workspace = workspaceRepository.findById(request.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + request.getWorkspaceId()));
        if (!workspace.isActive()) {
            throw new ResourceNotFoundException("Workspace not found with id: " + request.getWorkspaceId());
        }
        validateOwner(workspace.getId());

        Project project = new Project();
        project.setWorkspace(workspace);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        project.setStatus(request.getStatus() != null ? request.getStatus() : ProjectStatus.PLANNING);
        project.setStartDate(LocalDate.now());
        project.setEndDate(request.getEndDate());
        validateProjectDates(project);

        project = projectRepository.save(project);

        sendProjectNotification(project, NotificationType.PROJECT_CREATED,
                "Dự án mới: " + project.getName(),
                "Dự án \"" + project.getName() + "\" vừa được tạo trong workspace \"" + workspace.getName() + "\".");

        activityLogService.log(EntityType.PROJECT, project.getId(), ActivityAction.CREATED,
                null, null, project.getName(), getCurrentUserId(), workspace.getId());

        return toProjectResponse(project);
    }

    @Override
    @Transactional(readOnly = true)
	public ProjectResponse getById(Long id) {
		Project project = projectRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
		if (!project.isActive() || !project.getWorkspace().isActive()) {
			throw new ResourceNotFoundException("Project not found with id: " + id);
		}
		validateMembership(project.getWorkspace().getId());
		return toProjectResponse(project);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProjectResponse> getAll(Pageable pageable) {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		Page<Project> page = projectRepository.findAllActiveByWorkspaceIds(memberWorkspaceIds, pageable);
		List<ProjectResponse> content = page.getContent().stream()
				.map(this::toProjectResponse)
				.toList();
		return PageResponse.from(page, content);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProjectResponse> getAll(ProjectFilterRequest filter, Pageable pageable) {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		Specification<Project> spec = Specification
				.where(ProjectSpecifications.isActive())
				.and(ProjectSpecifications.inWorkspaces(memberWorkspaceIds))
				.and(ProjectSpecifications.fromFilter(filter));
		Page<Project> page = projectRepository.findAll(spec, pageable);
		List<ProjectResponse> content = page.getContent().stream()
				.map(this::toProjectResponse)
				.toList();
		return PageResponse.from(page, content);
	}

    @Override
    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        if (!project.isActive()) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }
        if (!project.getWorkspace().isActive()) {
            throw new ResourceNotFoundException("Workspace not found with id: " + project.getWorkspace().getId());
        }
        validateOwner(project.getWorkspace().getId());

        String oldName = project.getName();
        String oldDescription = project.getDescription();
        ProjectStatus oldStatus = project.getStatus();
        LocalDate oldEndDate = project.getEndDate();

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            project.setStatus(request.getStatus());
        }
        if (request.getEndDate() != null) {
            project.setEndDate(request.getEndDate());
        }
        validateProjectDates(project);

        project = projectRepository.save(project);

        Long currentUserId = getCurrentUserId();
        Long workspaceId = project.getWorkspace().getId();

        sendProjectNotification(project, NotificationType.PROJECT_UPDATED,
                "Dự án đã được cập nhật: " + project.getName(),
                "Dự án \"" + oldName + "\" đã được cập nhật trong workspace \"" + project.getWorkspace().getName() + "\".");

        if (request.getName() != null && !request.getName().equals(oldName)) {
            activityLogService.log(EntityType.PROJECT, project.getId(), ActivityAction.UPDATED,
                    "name", oldName, request.getName(), currentUserId, workspaceId);
        }
        if (request.getDescription() != null && !request.getDescription().equals(oldDescription)) {
            activityLogService.log(EntityType.PROJECT, project.getId(), ActivityAction.UPDATED,
                    "description", oldDescription, request.getDescription(), currentUserId, workspaceId);
        }
        if (request.getStatus() != null && !request.getStatus().equals(oldStatus)) {
            activityLogService.log(EntityType.PROJECT, project.getId(), ActivityAction.UPDATED,
                    "status", oldStatus.name(), request.getStatus().name(), currentUserId, workspaceId);
        }
        if (request.getEndDate() != null && !request.getEndDate().equals(oldEndDate)) {
            activityLogService.log(EntityType.PROJECT, project.getId(), ActivityAction.UPDATED,
                    "endDate",
                    oldEndDate != null ? oldEndDate.toString() : null,
                    request.getEndDate().toString(),
                    currentUserId, workspaceId);
        }

        return toProjectResponse(project);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id: " + id));
        if (!project.isActive()) {
            throw new ResourceNotFoundException("Project not found with id: " + id);
        }
        if (!project.getWorkspace().isActive()) {
            throw new ResourceNotFoundException("Workspace not found with id: " + project.getWorkspace().getId());
        }
        validateOwner(project.getWorkspace().getId());

        String projectName = project.getName();
        String workspaceName = project.getWorkspace().getName();
        Long workspaceId = project.getWorkspace().getId();

        project.setActive(false);
        projectRepository.save(project);

        Long currentUserId = getCurrentUserId();

        activityLogService.log(EntityType.PROJECT, project.getId(), ActivityAction.DELETED,
                null, null, projectName, currentUserId, workspaceId);

        sendProjectNotification(project, NotificationType.PROJECT_DELETED,
                "Dự án đã bị xóa: " + projectName,
                "Dự án \"" + projectName + "\" đã bị xóa khỏi workspace \"" + workspaceName + "\".");
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private Set<Long> getMemberWorkspaceIds() {
        Long currentUserId = getCurrentUserId();
        return workspaceMemberRepository.findByUserId(currentUserId).stream()
                .map(m -> m.getWorkspace().getId())
                .collect(Collectors.toSet());
    }

    private void validateMembership(Long workspaceId) {
        Long currentUserId = getCurrentUserId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserId)) {
            throw new ResourceNotFoundException("Project not found");
        }
    }

    private void validateOwner(Long workspaceId) {
        Long currentUserId = getCurrentUserId();
        boolean isOwner = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
                .filter(owner -> owner.getUser().getId().equals(currentUserId))
                .isPresent();
        if (!isOwner) {
            throw new InvalidOperationException("Only workspace owner can perform this action.");
        }
    }

    private void sendProjectNotification(Project project, NotificationType type, String title, String content) {
        Workspace workspace = project.getWorkspace();
        Long senderId = getCurrentUserId();
        Long ownerId = workspaceMemberRepository.findByWorkspaceIdAndRole(workspace.getId(), WorkspaceRole.OWNER)
                .map(m -> m.getUser().getId()).orElse(null);

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspace.getId());
        for (WorkspaceMember member : members) {
            if (member.getUser().getId().equals(senderId)) {
                if (!member.getUser().getId().equals(ownerId)) {
                    continue;
                }
            }
            notificationService.createNotification(
                    title, content, type,
                    member.getUser().getId(),
                    senderId,
                    null,
                    project.getId(),
                    workspace.getId()
            );
        }
    }

    private void validateProjectDates(Project project) {
        if (project.getEndDate() != null && project.getEndDate().isBefore(project.getStartDate())) {
            throw new BusinessException(
                    "Project end date must be after or equal to start date (" + project.getStartDate() + ").");
        }
    }

    private ProjectResponse toProjectResponse(Project project) {
        return ProjectResponse.builder()
                .id(project.getId())
                .workspaceId(project.getWorkspace().getId())
                .workspaceName(project.getWorkspace().getName())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .taskCount((int) project.getTasks().stream().filter(Task::isActive).count())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}
