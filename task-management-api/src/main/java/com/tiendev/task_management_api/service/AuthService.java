package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.AuthResponse;
import com.tiendev.task_management_api.dto.RegisterResponse;
import com.tiendev.task_management_api.dto.request.LoginRequest;
import com.tiendev.task_management_api.dto.request.ResendVerificationRequest;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;
import com.tiendev.task_management_api.dto.request.VerifyEmailRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    RegisterResponse register(UserCreateRequest request);
    AuthResponse verifyEmail(VerifyEmailRequest request);
    void resendVerification(ResendVerificationRequest request);
    AuthResponse refreshToken(String refreshTokenValue);
    void logout(String refreshTokenValue);
}

