package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.config.JwtUtil;
import com.tiendev.task_management_api.dto.AuthResponse;
import com.tiendev.task_management_api.dto.RegisterResponse;
import com.tiendev.task_management_api.dto.request.LoginRequest;
import com.tiendev.task_management_api.dto.request.ResendVerificationRequest;
import com.tiendev.task_management_api.dto.request.UserCreateRequest;
import com.tiendev.task_management_api.dto.request.VerifyEmailRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.exception.InvalidOtpException;
import com.tiendev.task_management_api.event.RegistrationOtpEvent;
import com.tiendev.task_management_api.model.EmailVerificationCode;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.repository.EmailVerificationCodeRepository;
import com.tiendev.task_management_api.repository.RefreshTokenRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationCodeRepository verificationCodeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("testuser");
        sampleUser.setEmail("test@example.com");
        sampleUser.setPassword("encoded_pwd");
        sampleUser.setRole(Role.USER);
        sampleUser.setEmailVerified(false);
    }

    @Test
    void register_CreatesUnverifiedUser_SendsOtp_ReturnsEmailAndExpiresAtWithoutJwt() {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setEmail("Test@Example.COM");
        request.setPassword("password123");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(i -> "encoded_" + i.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(1L);
            return u;
        });

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("test@example.com", response.getEmail());
        assertNotNull(response.getExpiresAt());

        // Verify user saved with emailVerified = false
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertFalse(userCaptor.getValue().isEmailVerified());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());

        // Verify OTP code saved
        verify(verificationCodeRepository).save(any(EmailVerificationCode.class));

        // Verify OTP email sent
        verify(eventPublisher).publishEvent(any(RegistrationOtpEvent.class));
    }

    @Test
    void verifyEmail_CorrectOtp_MarksUserVerifiedAndIssuesTokens() {
        VerifyEmailRequest request = new VerifyEmailRequest("test@example.com", "123456");

        EmailVerificationCode code = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("hashed_123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(code));
        when(passwordEncoder.matches("123456", "hashed_123456")).thenReturn(true);
        when(jwtUtil.generateAccessToken(1L, "USER", "test@example.com")).thenReturn("valid-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("valid-refresh-token");

        AuthResponse authResponse = authService.verifyEmail(request);

        assertNotNull(authResponse);
        assertEquals("valid-access-token", authResponse.getAccessToken());
        assertNotNull(code.getUsedAt(), "OTP must be marked as used");
        assertTrue(sampleUser.isEmailVerified(), "User must be marked verified");
        verify(verificationCodeRepository).save(code);
        verify(userRepository).save(sampleUser);
    }

    @Test
    void verifyEmail_IncorrectOtp_IncrementsAttemptCount() {
        VerifyEmailRequest request = new VerifyEmailRequest("test@example.com", "999999");

        EmailVerificationCode code = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("hashed_123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .attemptCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(code));
        when(passwordEncoder.matches("999999", "hashed_123456")).thenReturn(false);

        InvalidOtpException ex = assertThrows(InvalidOtpException.class, () -> authService.verifyEmail(request));
        assertEquals("OTP_INVALID", ex.getErrorCode());
        assertEquals(1, code.getAttemptCount());
        verify(verificationCodeRepository).save(code);
        assertFalse(sampleUser.isEmailVerified());
    }

    @Test
    void verifyEmail_ExpiredOtp_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest("test@example.com", "123456");

        EmailVerificationCode code = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("hashed_123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(6))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(code));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals("OTP_EXPIRED", ex.getErrorCode());
    }

    @Test
    void verifyEmail_AlreadyUsedOtp_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest("test@example.com", "123456");

        EmailVerificationCode code = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("hashed_123456")
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .usedAt(LocalDateTime.now().minusMinutes(1))
                .attemptCount(0)
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(code));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals("OTP_ALREADY_USED", ex.getErrorCode());
    }

    @Test
    void verifyEmail_ExceededMaxAttempts_ThrowsBusinessException() {
        VerifyEmailRequest request = new VerifyEmailRequest("test@example.com", "123456");

        EmailVerificationCode code = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("hashed_123456")
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .attemptCount(5)
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(code));

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.verifyEmail(request));
        assertEquals("OTP_MAX_ATTEMPTS_EXCEEDED", ex.getErrorCode());
    }

    @Test
    void resendVerification_AppliesCooldownWithGenericResponse() {
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");

        EmailVerificationCode recentCode = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("hashed_123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .createdAt(LocalDateTime.now().minusSeconds(30)) // 30s ago < 60s
                .build();

        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(recentCode));

        assertDoesNotThrow(() -> authService.resendVerification(request));
        verify(eventPublisher, never()).publishEvent(any(RegistrationOtpEvent.class));
    }

    @Test
    void resendVerification_InvalidatesOldCodesAndGeneratesNew() {
        ResendVerificationRequest request = new ResendVerificationRequest("test@example.com");

        EmailVerificationCode oldCode = EmailVerificationCode.builder()
                .id(10L)
                .user(sampleUser)
                .codeHash("old_hashed_code")
                .expiresAt(LocalDateTime.now().plusMinutes(3))
                .createdAt(LocalDateTime.now().minusSeconds(70)) // > 60s
                .build();

        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(verificationCodeRepository.findByUserIdOrderByCreatedAtDescWithLock(1L)).thenReturn(List.of(oldCode));
        when(passwordEncoder.encode(anyString())).thenReturn("new_hash");

        authService.resendVerification(request);

        assertNotNull(oldCode.getUsedAt(), "Old code must be invalidated");
        verify(verificationCodeRepository, atLeast(2)).save(any(EmailVerificationCode.class));
        verify(eventPublisher).publishEvent(any(RegistrationOtpEvent.class));
    }

    @Test
    void resendVerification_GenericResponseForNonExistingOrVerifiedUser() {
        // Unknown user
        when(userRepository.findByEmailForUpdate("unknown@example.com")).thenReturn(Optional.empty());
        assertDoesNotThrow(() -> authService.resendVerification(new ResendVerificationRequest("unknown@example.com")));

        // Verified user
        sampleUser.setEmailVerified(true);
        when(userRepository.findByEmailForUpdate("test@example.com")).thenReturn(Optional.of(sampleUser));
        assertDoesNotThrow(() -> authService.resendVerification(new ResendVerificationRequest("test@example.com")));
        verify(emailService, never()).sendRegistrationOtp(anyString(), anyString(), anyInt());
    }

    @Test
    void login_UnverifiedUser_ThrowsEmailNotVerifiedException() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encoded_pwd")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.login(request));
        assertEquals("EMAIL_NOT_VERIFIED", ex.getErrorCode());
    }
}
