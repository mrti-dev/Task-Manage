package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.WorkspaceInvitationResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceInvitationCreateRequest;

import java.util.List;

public interface WorkspaceInvitationService {

    WorkspaceInvitationResponse createInvitation(Long workspaceId, WorkspaceInvitationCreateRequest request);

    List<WorkspaceInvitationResponse> getWorkspaceInvitations(Long workspaceId);

    WorkspaceInvitationResponse resendInvitation(Long invitationId);
}
