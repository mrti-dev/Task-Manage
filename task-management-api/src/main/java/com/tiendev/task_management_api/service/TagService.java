package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.TagResponse;
import com.tiendev.task_management_api.dto.TaskResponse;
import com.tiendev.task_management_api.dto.request.TagCreateRequest;
import com.tiendev.task_management_api.dto.request.TagUpdateRequest;

import java.util.List;

public interface TagService {

    TagResponse createTag(Long workspaceId, TagCreateRequest request);

    List<TagResponse> getWorkspaceTags(Long workspaceId);

    TagResponse updateTag(Long tagId, TagUpdateRequest request);

    void deleteTag(Long tagId);

    TaskResponse attachTag(Long taskId, Long tagId);

    TaskResponse detachTag(Long taskId, Long tagId);
}
