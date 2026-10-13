package com.tiendev.task_management_api.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tiendev.task_management_api.config.JwtUtil;
import com.tiendev.task_management_api.dto.AuthResponse;
import com.tiendev.task_management_api.dto.RegisterResponse;
import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.LoginRequest;
import com.tiendev.task_management_api.dto.request.ResendVerificationRequest;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;
import com.tiendev.task_management_api.dto.request.VerifyEmailRequest;
import com.tiendev.task_management_api.event.RegistrationOtpEvent;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOtpException;
import com.tiendev.task_management_api.exception.ResourceAlreadyExistsException;
import com.tiendev.task_management_api.exception.ResourceNotFoundException;
import com.tiendev.task_management_api.exception.UnauthorizedException;
import com.tiendev.task_management_api.model.EmailVerificationCode;
import com.tiendev.task_management_api.model.RefreshToken;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.repository.EmailVerificationCodeRepository;
import com.tiendev.task_management_api.repository.RefreshTokenRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.service.AuthService;
import com.tiendev.task_management_api.service.EmailService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final EmailVerificationCodeRepository verificationCodeRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final EmailService emailService;
	private final ApplicationEventPublisher eventPublisher;

	@Value("${app.auth.otp.expiration-minutes:5}")
	private int otpExpirationMinutes = 5;

	@Value("${app.auth.otp.cooldown-seconds:60}")
	private int otpCooldownSeconds = 60;

	@Value("${app.auth.otp.max-attempts:5}")
	private int otpMaxAttempts = 5;

	@Override
	@Transactional
	public AuthResponse login(LoginRequest request) {
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid username or password"));

		if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new ResourceNotFoundException("Invalid username or password");
		}

		if (!user.isEmailVerified()) {
			throw new BusinessException("Email chưa được xác minh. Vui lòng xác minh email trước khi đăng nhập.", "EMAIL_NOT_VERIFIED");
		}

		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public RegisterResponse register(UserCreateRequest request) {
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		String trimmedUsername = request.getUsername().trim();

		if (userRepository.existsByEmail(normalizedEmail)) {
			throw new ResourceAlreadyExistsException("Email already exists: " + normalizedEmail);
		}

		User user = new User();
		user.setUsername(trimmedUsername);
		user.setEmail(normalizedEmail);
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setAvatar(request.getAvatar());
		user.setRole(Role.USER);
		user.setEmailVerified(false);

		user = userRepository.save(user);

		String rawOtp = generateOtp();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

		EmailVerificationCode verificationCode = EmailVerificationCode.builder()
				.user(user)
				.codeHash(passwordEncoder.encode(rawOtp))
				.expiresAt(expiresAt)
				.attemptCount(0)
				.createdAt(LocalDateTime.now())
				.build();
		verificationCodeRepository.save(verificationCode);

		eventPublisher.publishEvent(new RegistrationOtpEvent(user.getEmail(), rawOtp, otpExpirationMinutes));

		return RegisterResponse.builder()
				.email(user.getEmail())
				.expiresAt(expiresAt)
				.build();
	}

	@Override
	@Transactional(noRollbackFor = InvalidOtpException.class)
	public AuthResponse verifyEmail(VerifyEmailRequest request) {
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		User user = userRepository.findByEmail(normalizedEmail)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + normalizedEmail));

		List<EmailVerificationCode> codes = verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(user.getId());
		if (codes.isEmpty()) {
			throw new BusinessException("Không tìm thấy mã xác minh cho tài khoản này.", "OTP_NOT_FOUND");
		}

		EmailVerificationCode code = codes.get(0);

		if (code.getUsedAt() != null) {
			throw new BusinessException("Mã xác minh đã được sử dụng.", "OTP_ALREADY_USED");
		}

		if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new BusinessException("Mã xác minh đã hết hạn.", "OTP_EXPIRED");
		}

		if (code.getAttemptCount() >= otpMaxAttempts) {
			throw new BusinessException("Đã vượt quá số lần thử tối đa. Vui lòng gửi lại mã mới.", "OTP_MAX_ATTEMPTS_EXCEEDED");
		}

		if (!passwordEncoder.matches(request.getCode().trim(), code.getCodeHash())) {
			code.setAttemptCount(code.getAttemptCount() + 1);
			verificationCodeRepository.save(code);
			throw new InvalidOtpException();
		}

		code.setUsedAt(LocalDateTime.now());
		verificationCodeRepository.save(code);

		user.setEmailVerified(true);
		userRepository.save(user);

		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public void resendVerification(ResendVerificationRequest request) {
		String normalizedEmail = request.getEmail().trim().toLowerCase();
		Optional<User> userOpt = userRepository.findByEmailForUpdate(normalizedEmail);
		if (userOpt.isEmpty() || userOpt.get().isEmailVerified()) {
			return;
		}

		User user = userOpt.get();
		List<EmailVerificationCode> codes = verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(user.getId());
		if (!codes.isEmpty()) {
			EmailVerificationCode latest = codes.get(0);
			if (latest.getCreatedAt().plusSeconds(otpCooldownSeconds).isAfter(LocalDateTime.now())) {
				return;
			}
		}

		for (EmailVerificationCode oldCode : codes) {
			if (oldCode.getUsedAt() == null) {
				oldCode.setUsedAt(LocalDateTime.now());
				verificationCodeRepository.save(oldCode);
			}
		}

		String rawOtp = generateOtp();
		LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(otpExpirationMinutes);

		EmailVerificationCode newCode = EmailVerificationCode.builder()
				.user(user)
				.codeHash(passwordEncoder.encode(rawOtp))
				.expiresAt(expiresAt)
				.attemptCount(0)
				.createdAt(LocalDateTime.now())
				.build();
		verificationCodeRepository.save(newCode);

		eventPublisher.publishEvent(new RegistrationOtpEvent(user.getEmail(), rawOtp, otpExpirationMinutes));
	}

	@Override
	@Transactional
	public AuthResponse refreshToken(String refreshTokenValue) {
		RefreshToken storedToken = refreshTokenRepository.findByTokenWithLock(refreshTokenValue)
				.orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

		if (storedToken.isRevoked()) {
			throw new UnauthorizedException("Refresh token has been revoked");
		}

		if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new UnauthorizedException("Refresh token has expired");
		}

		storedToken.setRevoked(true);
		refreshTokenRepository.save(storedToken);

		User user = storedToken.getUser();
		if (!user.isEmailVerified()) {
			throw new UnauthorizedException("Email verification is required");
		}
		return buildAuthResponse(user);
	}

	@Override
	@Transactional
	public void logout(String refreshTokenValue) {
		if (refreshTokenValue != null) {
			refreshTokenRepository.findByTokenWithLock(refreshTokenValue).ifPresent(token -> {
				token.setRevoked(true);
				refreshTokenRepository.save(token);
			});
		}
	}

	private String generateOtp() {
		SecureRandom random = new SecureRandom();
		int code = random.nextInt(1_000_000);
		return String.format("%06d", code);
	}

	private AuthResponse buildAuthResponse(User user) {
		String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole().name(), user.getEmail());
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
