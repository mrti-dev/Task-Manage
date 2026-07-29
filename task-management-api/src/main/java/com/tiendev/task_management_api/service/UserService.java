package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;
import com.tiendev.task_management_api.dto.request.UserUpdateRequest;

import java.util.List;

public interface UserService {
    UserResponse create(UserCreateRequest request);
    UserResponse getById(Long id);
    List<UserResponse> getAll();
    UserResponse update(Long id, UserUpdateRequest request);
    UserResponse getCurrentUser();
    UserResponse updateCurrentUser(UserUpdateRequest request);
    void delete(Long id);
}
