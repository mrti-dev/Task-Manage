package com.tiendev.task_management_api.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.WorkspaceResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceCreateRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceUpdateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Project;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.WorkspaceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkspaceServiceImpl implements WorkspaceService {

	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public WorkspaceResponse create(WorkspaceCreateRequest request) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		Long currentUserId = Long.valueOf(auth.getName());
		User owner = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

		Workspace workspace = new Workspace();
		workspace.setName(request.getName());
		workspace.setDescription(request.getDescription());
		workspace = workspaceRepository.save(workspace);

		WorkspaceMember ownerMember = new WorkspaceMember();
		ownerMember.setWorkspace(workspace);
		ownerMember.setUser(owner);
		ownerMember.setRole(WorkspaceRole.OWNER);
		workspace.getMembers().add(ownerMember);

		workspace = workspaceRepository.save(workspace);
		return toWorkspaceResponse(workspace);
	}

	@Override
	@Transactional(readOnly = true)
	public WorkspaceResponse getById(Long id) {
		Long currentUserId = getCurrentUserId();
		Workspace workspace = workspaceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
		if (!workspace.isActive()) {
			throw new ResourceNotFoundException("Workspace not found with id: " + id);
		}
		if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(id, currentUserId)) {
			throw new ResourceNotFoundException("Workspace not found with id: " + id);
		}
		return toWorkspaceResponse(workspace);
	}

	@Override
	@Transactional(readOnly = true)
	public List<WorkspaceResponse> getAll() {
		Set<Long> memberWorkspaceIds = getMemberWorkspaceIds();
		return workspaceRepository.findAll().stream()
				.filter(w -> w.isActive() && memberWorkspaceIds.contains(w.getId()))
				.map(this::toWorkspaceResponse)
				.toList();
	}

	@Override
	@Transactional
	public WorkspaceResponse update(Long id, WorkspaceUpdateRequest request) {
		Workspace workspace = workspaceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
		if (!workspace.isActive()) {
			throw new ResourceNotFoundException("Workspace not found with id: " + id);
		}
		validateOwner(workspace.getId());

		if (request.getName() != null) {
			workspace.setName(request.getName());
		}
		if (request.getDescription() != null) {
			workspace.setDescription(request.getDescription());
		}

		workspace = workspaceRepository.save(workspace);
		return toWorkspaceResponse(workspace);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		Workspace workspace = workspaceRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + id));
		if (!workspace.isActive()) {
			throw new ResourceNotFoundException("Workspace not found with id: " + id);
		}
		validateOwner(workspace.getId());
		workspace.setActive(false);
		workspaceRepository.save(workspace);
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

	private void validateOwner(Long workspaceId) {
		Long currentUserId = getCurrentUserId();
		boolean isOwner = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER)
				.filter(owner -> owner.getUser().getId().equals(currentUserId))
				.isPresent();
		if (!isOwner) {
			throw new InvalidOperationException("Only workspace owner can perform this action.");
		}
	}

	private WorkspaceResponse toWorkspaceResponse(Workspace workspace) {
		UserResponse ownerResponse = workspace.getMembers().stream().filter(m -> m.getRole() == WorkspaceRole.OWNER)
				.findFirst().map(m -> UserResponse.builder().id(m.getUser().getId()).username(m.getUser().getUsername())
						.email(m.getUser().getEmail()).avatar(m.getUser().getAvatar()).role(m.getUser().getRole())
						.createdAt(m.getUser().getCreatedAt()).updatedAt(m.getUser().getUpdatedAt()).build())
				.orElse(null);

		return WorkspaceResponse.builder().id(workspace.getId()).name(workspace.getName())
				.description(workspace.getDescription()).owner(ownerResponse)
				.memberCount(workspace.getMembers().size())
				.projectCount((int) workspace.getProjects().stream().filter(Project::isActive).count())
				.createdAt(workspace.getCreatedAt())
				.updatedAt(workspace.getUpdatedAt()).build();
	}
}
