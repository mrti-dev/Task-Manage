package com.tiendev.task_management_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Pattern WORKSPACE_TOPIC = Pattern.compile("^/topic/workspace/(\\d+)(?:/.*)?$");

    private final JwtUtil jwtUtil;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new AccessDeniedException("Authentication required");
            }
            try {
                String token = authHeader.substring(7);
                jwtUtil.validateToken(token);
                Long userId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                accessor.setUser(new UsernamePasswordAuthenticationToken(String.valueOf(userId), null, authorities));
            } catch (Exception e) {
                throw new AccessDeniedException("Invalid JWT token", e);
            }
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            if (accessor.getUser() == null) {
                throw new AccessDeniedException("Authentication required");
            }
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith("/topic/")) {
                Matcher matcher = WORKSPACE_TOPIC.matcher(destination);
                if (!matcher.matches() || !workspaceMemberRepository.existsByWorkspaceIdAndUserId(
                        Long.valueOf(matcher.group(1)), Long.valueOf(accessor.getUser().getName()))) {
                    throw new AccessDeniedException("Workspace access denied");
                }
            }
        }

        return message;
    }
}
