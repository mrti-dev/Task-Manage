package com.tiendev.task_management_api.integration;

import com.tiendev.task_management_api.dto.request.TransferOwnershipRequest;
import com.tiendev.task_management_api.dto.request.WorkspaceInvitationCreateRequest;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.WorkspaceInvitationService;
import com.tiendev.task_management_api.service.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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
class OwnershipAndInvitationConcurrencyTest {
    @Autowired private WorkspaceService workspaceService;
    @Autowired private WorkspaceInvitationService invitationService;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository memberRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void concurrentOwnershipTransfersLeaveExactlyOneOwner() throws Exception {
        User owner = user("owner-race");
        User first = user("first-race");
        User second = user("second-race");
        Workspace workspace = workspace("owner-race");
        member(workspace, owner, WorkspaceRole.OWNER);
        member(workspace, first, WorkspaceRole.MEMBER);
        member(workspace, second, WorkspaceRole.MEMBER);
        workspace = workspaceRepository.saveAndFlush(workspace);

        Long workspaceId = workspace.getId();
        int succeeded = race(
                as(owner.getId(), () -> workspaceService.transferOwnership(workspaceId, new TransferOwnershipRequest(first.getId()))),
                as(owner.getId(), () -> workspaceService.transferOwnership(workspaceId, new TransferOwnershipRequest(second.getId()))));

        assertEquals(1, succeeded);
        assertEquals(1, memberRepository.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.OWNER));
    }

    @Test
    void concurrentDirectAddCreatesOneMembership() throws Exception {
        User inviter = user("inviter-race");
        User recipient = user("recipient-race");
        Workspace workspace = workspace("invitation-race");
        member(workspace, inviter, WorkspaceRole.OWNER);
        workspace = workspaceRepository.saveAndFlush(workspace);
        Long workspaceId = workspace.getId();
        WorkspaceInvitationCreateRequest request = new WorkspaceInvitationCreateRequest(recipient.getEmail(), WorkspaceRole.MEMBER);

        int succeeded = race(
                as(inviter.getId(), () -> invitationService.createInvitation(workspaceId, request)),
                as(inviter.getId(), () -> invitationService.createInvitation(workspaceId, request)));

        assertEquals(1, succeeded);
        assertEquals(1, memberRepository.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.MEMBER));
    }

    private int race(Callable<?> first, Callable<?> second) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> results = List.of(first, second).stream().map(action -> executor.submit(() -> {
                ready.countDown();
                start.await();
                try {
                    action.call();
                    return true;
                } catch (RuntimeException ex) {
                    return false;
                }
            })).toList();
            ready.await();
            start.countDown();
            return (int) results.stream().filter(result -> {
                try {
                    return result.get();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).count();
        }
    }

    private Callable<Object> as(Long userId, Callable<?> action) {
        return () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(userId.toString(), null));
            try {
                return action.call();
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private User user(String suffix) {
        User user = new User();
        user.setUsername(suffix);
        user.setEmail(suffix + "@example.com");
        user.setPassword("password123");
        user.setRole(Role.USER);
        return userRepository.saveAndFlush(user);
    }

    private Workspace workspace(String name) {
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setActive(true);
        return workspaceRepository.saveAndFlush(workspace);
    }

    private void member(Workspace workspace, User user, WorkspaceRole role) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setRole(role);
        memberRepository.save(member);
    }
}
