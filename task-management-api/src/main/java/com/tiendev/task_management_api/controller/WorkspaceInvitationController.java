package com.tiendev.task_management_api.controller;

import com.tiendev.task_management_api.dto.WorkspaceInvitationResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceInvitationCreateRequest;
import com.tiendev.task_management_api.helper.ApiResponse;
import com.tiendev.task_management_api.service.WorkspaceInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class WorkspaceInvitationController {

    private final WorkspaceInvitationService invitationService;

    @PostMapping("/api/workspaces/{workspaceId}/invitations")
    public ResponseEntity<ApiResponse<WorkspaceInvitationResponse>> create(
            @PathVariable Long workspaceId,
            @Valid @RequestBody WorkspaceInvitationCreateRequest request) {
        WorkspaceInvitationResponse response = invitationService.createInvitation(workspaceId, request);
        return ApiResponse.created(response);
    }

    @GetMapping("/api/workspaces/{workspaceId}/invitations")
    public ResponseEntity<ApiResponse<List<WorkspaceInvitationResponse>>> getWorkspaceInvitations(
            @PathVariable Long workspaceId) {
        List<WorkspaceInvitationResponse> responses = invitationService.getWorkspaceInvitations(workspaceId);
        return ApiResponse.success(responses);
    }

    @PostMapping("/api/workspace-invitations/{id}/resend")
    public ResponseEntity<ApiResponse<WorkspaceInvitationResponse>> resend(@PathVariable Long id) {
        WorkspaceInvitationResponse response = invitationService.resendInvitation(id);
        return ApiResponse.success(response, "Workspace access notice queued");
    }

}
