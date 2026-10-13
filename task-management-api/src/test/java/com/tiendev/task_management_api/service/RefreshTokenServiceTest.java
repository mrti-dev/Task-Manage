package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.config.JwtUtil;
import com.tiendev.task_management_api.dto.AuthResponse;
import com.tiendev.task_management_api.exception.UnauthorizedException;
import com.tiendev.task_management_api.model.RefreshToken;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.repository.RefreshTokenRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import com.tiendev.task_management_api.repository.EmailVerificationCodeRepository;
import com.tiendev.task_management_api.service.EmailService;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationCodeRepository verificationCodeRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private EmailService emailService;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void successfullyRefreshesTokenAndRevokesCurrentOne() {
        String rawToken = "valid-token-abc";
        User user = new User();
        user.setId(10L);
        user.setRole(Role.USER);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setEmailVerified(true);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(rawToken);
        storedToken.setUser(user);
        storedToken.setRevoked(false);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(5));

        when(refreshTokenRepository.findByTokenWithLock(rawToken)).thenReturn(Optional.of(storedToken));
        when(jwtUtil.generateAccessToken(10L, "USER", "alice@example.com")).thenReturn("new-access-token");
        when(jwtUtil.generateRefreshToken()).thenReturn("new-refresh-token");

        AuthResponse response = authService.refreshToken(rawToken);

        assertNotNull(response);
        assertEquals("new-access-token", response.getAccessToken());
        assertTrue(storedToken.isRevoked(), "The refreshed token must be marked as revoked");
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void throwsUnauthorizedExceptionWhenTokenIsAlreadyRevoked() {
        String rawToken = "already-revoked-token";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(rawToken);
        storedToken.setRevoked(true);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(2));

        when(refreshTokenRepository.findByTokenWithLock(rawToken)).thenReturn(Optional.of(storedToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken(rawToken));
        assertEquals("Refresh token has been revoked", ex.getMessage());
    }

    @Test
    void throwsUnauthorizedExceptionWhenTokenIsExpired() {
        String rawToken = "expired-token";
        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(rawToken);
        storedToken.setRevoked(false);
        storedToken.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(refreshTokenRepository.findByTokenWithLock(rawToken)).thenReturn(Optional.of(storedToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken(rawToken));
        assertEquals("Refresh token has expired", ex.getMessage());
    }

    @Test
    void throwsUnauthorizedExceptionWhenTokenNotFound() {
        String rawToken = "non-existent-token";
        when(refreshTokenRepository.findByTokenWithLock(rawToken)).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken(rawToken));
        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void rejectsRefreshForUnverifiedUser() {
        User user = new User();
        user.setEmailVerified(false);
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        when(refreshTokenRepository.findByTokenWithLock("unverified-token")).thenReturn(Optional.of(storedToken));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken("unverified-token"));
        assertEquals("Email verification is required", ex.getMessage());
    }

    @Test
    void preventsConcurrentRefreshTokenReuse() {
        String rawToken = "concurrent-token";
        User user = new User();
        user.setId(5L);
        user.setRole(Role.USER);
        user.setUsername("bob");
        user.setEmail("bob@example.com");
        user.setEmailVerified(true);

        RefreshToken storedToken = new RefreshToken();
        storedToken.setToken(rawToken);
        storedToken.setUser(user);
        storedToken.setRevoked(false);
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(3));

        when(refreshTokenRepository.findByTokenWithLock(rawToken)).thenReturn(Optional.of(storedToken));
        when(jwtUtil.generateAccessToken(5L, "USER", "bob@example.com")).thenReturn("token-first-call");
        when(jwtUtil.generateRefreshToken()).thenReturn("refresh-first-call");

        // First call succeeds
        AuthResponse first = authService.refreshToken(rawToken);
        assertNotNull(first);
        assertTrue(storedToken.isRevoked());

        // Subsequent / concurrent call with the same token now sees revoked = true
        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.refreshToken(rawToken));
        assertEquals("Refresh token has been revoked", ex.getMessage());
    }
}
