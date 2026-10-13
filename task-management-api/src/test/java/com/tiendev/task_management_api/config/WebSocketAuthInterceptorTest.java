package com.tiendev.task_management_api.config;

import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks
    private WebSocketAuthInterceptor interceptor;

    @Test
    void rejectsConnectionWithoutToken() {
        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message(StompCommand.CONNECT, null), null));
    }

    @Test
    void rejectsSubscriptionToAnotherWorkspace() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/workspace/10/tasks/20/comments");
        accessor.setUser(new UsernamePasswordAuthenticationToken("7", null));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 7L)).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void allowsSubscriptionForWorkspaceMember() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/workspace/10/tasks/20/comments");
        accessor.setUser(new UsernamePasswordAuthenticationToken("7", null));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(workspaceMemberRepository.existsByWorkspaceIdAndUserId(10L, 7L)).thenReturn(true);

        assertDoesNotThrow(() -> interceptor.preSend(message, null));
    }

    private Message<byte[]> message(StompCommand command, String token) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (token != null) {
            accessor.setNativeHeader("Authorization", "Bearer " + token);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
