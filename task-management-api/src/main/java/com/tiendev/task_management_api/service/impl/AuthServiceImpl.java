package com.tiendev.task_management_api.service.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendev.task_management_api.config.JwtUtil;
import com.tiendev.task_management_api.dto.AuthResponse;
import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.LoginRequest;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;
import com.tiendev.task_management_api.exception.InvalidOperationException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.model.RefreshToken;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.repository.RefreshTokenRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;

	@Override
	@Transactional
	public AuthResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new ResourceNotFoundException("Invalid username or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new ResourceNotFoundException("Invalid username or password");
		}

		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public AuthResponse register(UserCreateRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new ResourceAlreadyExistsException("Email already exists: " + request.getEmail());
		}

		User user = new User();
		user.setUsername(request.getUsername());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setAvatar(request.getAvatar());
		user.setRole(Role.USER);

		user = userRepository.save(user);
		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public AuthResponse refreshToken(String refreshTokenValue) {
		RefreshToken storedToken = refreshTokenRepository.findByToken(refreshTokenValue)
				.orElseThrow(() -> new InvalidOperationException("Invalid refresh token"));

		if (storedToken.isRevoked()) {
			throw new InvalidOperationException("Refresh token has been revoked");
		}

		if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new InvalidOperationException("Refresh token has expired");
		}

		storedToken.setRevoked(true);
		refreshTokenRepository.save(storedToken);

		User user = storedToken.getUser();
		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public void logout(String refreshTokenValue) {
		if (refreshTokenValue != null) {
			refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
				token.setRevoked(true);
				refreshTokenRepository.save(token);
			});
		}
	}

	private AuthResponse buildAuthResponse(User user) {
		String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
		String refreshTokenValue = jwtUtil.generateRefreshToken();

		RefreshToken refreshToken = new RefreshToken();
		refreshToken.setToken(refreshTokenValue);
		refreshToken.setUser(user);
		refreshToken.setExpiresAt(LocalDateTime.now().plusDays(7));
		refreshToken.setRevoked(false);
		refreshTokenRepository.save(refreshToken);

		UserResponse userResponse = UserResponse.builder().id(user.getId()).username(user.getUsername())
				.email(user.getEmail()).avatar(user.getAvatar()).role(user.getRole()).createdAt(user.getCreatedAt())
				.updatedAt(user.getUpdatedAt()).build();

		return AuthResponse.builder().accessToken(accessToken).user(userResponse).refreshToken(refreshTokenValue)
				.build();
	}
}
