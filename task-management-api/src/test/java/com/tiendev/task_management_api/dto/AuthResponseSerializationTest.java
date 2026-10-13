package com.tiendev.task_management_api.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthResponseSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authResponseDoesNotSerializeRefreshTokenField() throws Exception {
        UserResponse userResponse = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("header.payload.signature")
                .refreshToken("sensitive-refresh-token-secret")
                .user(userResponse)
                .build();

        String json = objectMapper.writeValueAsString(authResponse);

        assertFalse(json.contains("refreshToken"), "JSON serialization must not include refreshToken field");
        assertFalse(json.contains("sensitive-refresh-token-secret"), "JSON serialization must not leak refreshToken value");

        JsonNode tree = objectMapper.readTree(json);
        assertNull(tree.get("refreshToken"), "refreshToken property should not exist in JSON node tree");
        assertTrue(tree.has("accessToken"), "accessToken property must exist");
        assertTrue(tree.has("user"), "user property must exist");
    }

    @Test
    void authResponseToStringDoesNotLeakRefreshToken() {
        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("secret-refresh-value-xyz")
                .build();

        String str = authResponse.toString();
        assertFalse(str.contains("secret-refresh-value-xyz"), "toString() must exclude refreshToken value");
    }
}
