package com.tiendev.task_management_api.service.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;
import com.tiendev.task_management_api.dto.request.UserUpdateRequest;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public UserResponse create(UserCreateRequest request) {
		if (userRepository.existsByUsername(request.getUsername())) {
			throw new ResourceAlreadyExistsException("Username already exists: " + request.getUsername());
		}
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResourceAlreadyExistsException("Email already exists: " + request.getEmail());
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setAvatar(request.getAvatar());
		user.setRole(
				request.getRole() != null ? request.getRole() : com.tiendev.task_management_api.model.enums.Role.USER);

		user = userRepository.save(user);
		return toUserResponse(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getById(Long id) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
		return toUserResponse(user);
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserResponse> getAll() {
		return userRepository.findAll().stream().map(this::toUserResponse).toList();
	}

	@Override
	@Transactional
	public UserResponse update(Long id, UserUpdateRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

		if (request.getUsername() != null) {
			if (!request.getUsername().equals(user.getUsername())
					&& userRepository.existsByUsername(request.getUsername())) {
				throw new ResourceAlreadyExistsException("Username already exists: " + request.getUsername());
			}
			user.setUsername(request.getUsername());
		}
		if (request.getEmail() != null) {
			if (!request.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
				throw new ResourceAlreadyExistsException("Email already exists: " + request.getEmail());
			}
			user.setEmail(request.getEmail());
		}
		if (request.getPassword() != null) {
			user.setPassword(passwordEncoder.encode(request.getPassword()));
		}
		if (request.getAvatar() != null) {
			user.setAvatar(request.getAvatar());
		}
		if (request.getRole() != null) {
			user.setRole(request.getRole());
		}

		user = userRepository.save(user);
		return toUserResponse(user);
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getCurrentUser() {
		Long currentUserId = getCurrentUserId();
		return getById(currentUserId);
	}

	@Override
	@Transactional
	public UserResponse updateCurrentUser(UserUpdateRequest request) {
		Long currentUserId = getCurrentUserId();
		User user = userRepository.findById(currentUserId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + currentUserId));

		if (request.getUsername() != null) {
			if (request.getUsername().isBlank()) {
				throw new IllegalArgumentException("Username must not be blank");
			}
			user.setUsername(request.getUsername());
		}
		if (request.getAvatar() != null) {
			user.setAvatar(request.getAvatar());
		}

		user = userRepository.save(user);
		return toUserResponse(user);
	}

	@Override
	@Transactional
	public void delete(Long id) {
		if (!userRepository.existsById(id)) {
			throw new ResourceNotFoundException("User not found with id: " + id);
		}
		userRepository.deleteById(id);
	}

	private UserResponse toUserResponse(User user) {
		return UserResponse.builder().id(user.getId()).username(user.getUsername()).email(user.getEmail())
				.avatar(user.getAvatar()).role(user.getRole()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
	}

	private Long getCurrentUserId() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		return Long.valueOf(auth.getName());
	}
}
