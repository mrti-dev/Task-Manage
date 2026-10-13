package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.model.enums.WorkspaceRole;

public interface EmailService {

    void sendRegistrationOtp(String toEmail, String otp, int expirationMinutes);

    void sendWorkspaceMemberAdded(String toEmail, String inviterName, String workspaceName, WorkspaceRole role, String workspaceUrl);
}
