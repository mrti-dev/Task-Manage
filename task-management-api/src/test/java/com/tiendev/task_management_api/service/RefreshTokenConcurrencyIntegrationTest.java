package com.tiendev.task_management_api.service;

import com.tiendev.task_management_api.exception.UnauthorizedException;
import com.tiendev.task_management_api.model.RefreshToken;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.repository.RefreshTokenRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private UserRepository userRepository;

    @AfterEach
    void cleanUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void allowsOnlyOneConcurrentRefresh() throws Exception {
        User user = new User();
        user.setUsername("concurrent-user");
        user.setEmail("concurrent@example.com");
        user.setPassword("encoded-password");
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        user = userRepository.save(user);

        RefreshToken token = new RefreshToken();
        token.setToken("single-use-token");
        token.setUser(user);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        refreshTokenRepository.save(token);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> refresh = () -> {
            ready.countDown();
            start.await();
            try {
                authService.refreshToken("single-use-token");
                return true;
            } catch (UnauthorizedException ignored) {
                return false;
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = List.of(executor.submit(refresh), executor.submit(refresh));
            ready.await();
            start.countDown();
            int successes = 0;
            for (Future<Boolean> result : results) {
                if (result.get()) successes++;
            }
            assertEquals(1, successes);
        }
    }
}
