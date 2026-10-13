package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.dto.TagResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.request.TagCreateRequest;
import com.tiendev.task_management_api.dto.request.TagUpdateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.Tag;
import com.tiendev.task_management_api.model.Task;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.repository.TagRepository;
import com.tiendev.task_management_api.repository.TaskRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.TagService;
import com.tiendev.task_management_api.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final TaskRepository taskRepository;

    @Lazy
    private final TaskService taskService;

    @Override
    @Transactional
    public TagResponse createTag(Long workspaceId, TagCreateRequest request) {
        validateWorkspaceAccess(workspaceId);
        String name = request.getName().trim();

        if (tagRepository.existsByWorkspaceIdAndName(workspaceId, name)) {
            throw new ResourceAlreadyExistsException("A tag with name '" + name + "' already exists in this workspace");
        }

        Workspace workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found with id: " + workspaceId));

        Tag tag = new Tag();
        tag.setWorkspace(workspace);
        tag.setName(name);
        tag.setColor(request.getColor() != null && !request.getColor().isBlank() ? request.getColor() : "#3B82F6");

        tag = tagRepository.save(tag);
        return toResponse(tag);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagResponse> getWorkspaceTags(Long workspaceId) {
        validateWorkspaceAccess(workspaceId);
        return tagRepository.findByWorkspaceIdOrderByNameAsc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TagResponse updateTag(Long tagId, TagUpdateRequest request) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));
        validateWorkspaceAccess(tag.getWorkspace().getId());

        if (request.getName() != null && !request.getName().isBlank()) {
            String newName = request.getName().trim();
            if (!newName.equalsIgnoreCase(tag.getName())
                    && tagRepository.existsByWorkspaceIdAndName(tag.getWorkspace().getId(), newName)) {
                throw new ResourceAlreadyExistsException("A tag with name '" + newName + "' already exists in this workspace");
            }
            tag.setName(newName);
        }

        if (request.getColor() != null && !request.getColor().isBlank()) {
            tag.setColor(request.getColor());
        }

        tag = tagRepository.save(tag);
        return toResponse(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long tagId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));
        validateWorkspaceAccess(tag.getWorkspace().getId());

        // Remove tag associations from tasks
        List<Task> tasksWithTag = taskRepository.findByTags_Id(tagId);
        for (Task t : tasksWithTag) {
            t.getTags().remove(tag);
            taskRepository.save(t);
        }

        tagRepository.delete(tag);
    }

    @Override
    @Transactional
    public TaskResponse attachTag(Long taskId, Long tagId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        Long workspaceId = task.getProject().getWorkspace().getId();
        validateWorkspaceAccess(workspaceId);

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        if (!tag.getWorkspace().getId().equals(workspaceId)) {
            throw new InvalidOperationException("Tag does not belong to the same workspace as the task");
        }

        task.getTags().add(tag);
        task = taskRepository.save(task);
        return taskService.getById(taskId);
    }

    @Override
    @Transactional
    public TaskResponse detachTag(Long taskId, Long tagId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + taskId));
        Long workspaceId = task.getProject().getWorkspace().getId();
        validateWorkspaceAccess(workspaceId);

        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found with id: " + tagId));

        task.getTags().remove(tag);
        task = taskRepository.save(task);
        return taskService.getById(taskId);
    }

    private void validateWorkspaceAccess(Long workspaceId) {
        Long currentUserId = getCurrentUserId();
        if (!workspaceMemberRepository.existsByWorkspaceIdAndUserId(workspaceId, currentUserId)) {
            throw new InvalidOperationException("You do not have access to this workspace");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return Long.valueOf(auth.getName());
    }

    private TagResponse toResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .workspaceId(tag.getWorkspace().getId())
                .name(tag.getName())
                .color(tag.getColor())
                .build();
    }
}
