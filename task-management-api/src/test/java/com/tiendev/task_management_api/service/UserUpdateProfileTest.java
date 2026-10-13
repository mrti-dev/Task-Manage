package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.dto.UserResponse;
import com.tiendev.task_management_api.dto.request.UserUpdateRequest;
import com.tiendev.task_management_api.exception.BusinessException;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.service.impl.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserUpdateProfileTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private UserServiceImpl userService;

    private final Long userId = 5L;

    @BeforeEach
    void setupSecurity() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(String.valueOf(userId), null));
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsUpdatingProfileWhenUsernameIsUnchanged() {
        User user = new User();
        user.setId(userId);
        user.setUsername("current_username");
        user.setEmail("user@example.com");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("current_username")
                .avatar("https://example.com/avatar.png")
                .build();

        UserResponse response = userService.updateCurrentUser(request);

        assertNotNull(response);
        assertEquals("current_username", response.getUsername());
        assertEquals("https://example.com/avatar.png", response.getAvatar());
    }

    @Test
    void allowsUpdatingProfileToDuplicateUsername() {
        User user = new User();
        user.setId(userId);
        user.setUsername("current_username");
        user.setEmail("user@example.com");
        user.setRole(Role.USER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("taken_username")
                .build();

        UserResponse response = userService.updateCurrentUser(request);
        assertEquals("taken_username", response.getUsername());
    }

    @Test
    void throwsBusinessExceptionWhenUsernameIsBlank() {
        User user = new User();
        user.setId(userId);
        user.setUsername("current_username");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserUpdateRequest request = UserUpdateRequest.builder()
                .username("   ")
                .build();

        assertThrows(BusinessException.class, () -> userService.updateCurrentUser(request));
    }
}
