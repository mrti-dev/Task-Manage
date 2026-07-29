package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.AuthResponse;
import com.tiendev.task_management_api.dto.request.LoginRequest;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    AuthResponse register(UserCreateRequest request);
    AuthResponse refreshToken(String refreshTokenValue);
    void logout(String refreshTokenValue);
}
