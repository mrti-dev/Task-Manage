package com.tiendev.task_management_api.integration;

import com.tiendev.task_management_api.dto.WorkspaceInvitationResponse;
import com.tiendev.task_management_api.dto.request.WorkspaceInvitationCreateRequest;
import com.tiendev.task_management_api.dto.request.VerifyEmailRequest;
import com.tiendev.task_management_api.exception.InvalidOtpException;
import com.tiendev.task_management_api.model.Notification;
import com.tiendev.task_management_api.model.User;
import com.tiendev.task_management_api.model.Workspace;
import com.tiendev.task_management_api.model.WorkspaceMember;
import com.tiendev.task_management_api.model.enums.Role;
import com.tiendev.task_management_api.model.enums.NotificationType;
import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.repository.NotificationRepository;
import com.tiendev.task_management_api.repository.UserRepository;
import com.tiendev.task_management_api.repository.WorkspaceMemberRepository;
import com.tiendev.task_management_api.repository.WorkspaceRepository;
import com.tiendev.task_management_api.service.WorkspaceInvitationService;
import com.tiendev.task_management_api.service.AuthService;
import com.tiendev.task_management_api.service.MailOutbox;
import com.tiendev.task_management_api.model.EmailVerificationCode;
import com.tiendev.task_management_api.repository.EmailVerificationCodeRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.MailSendException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class EmailVerificationAndInvitationIntegrationTest {

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private WorkspaceInvitationService invitationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EmailVerificationCodeRepository verificationCodeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MailOutbox mailOutbox;

    @Autowired
    private JdbcTemplate jdbc;

    private User owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        MimeMessage mockMessage = Mockito.mock(MimeMessage.class);
        Mockito.when(mailSender.createMimeMessage()).thenReturn(mockMessage);

        owner = new User();
        owner.setUsername("owner_" + System.currentTimeMillis());
        owner.setEmail("owner_" + System.currentTimeMillis() + "@example.com");
        owner.setPassword("password123");
        owner.setRole(Role.USER);
        owner.setEmailVerified(true);
        owner = userRepository.save(owner);

        workspace = new Workspace();
        workspace.setName("Workspace " + System.currentTimeMillis());
        workspace.setActive(true);
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(owner);
        member.setRole(WorkspaceRole.OWNER);
        memberRepository.save(member);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getId(), null, List.of())
        );
    }

    @Test
    void memberAddedImmediately_AndResendQueuesAccessNotice() {
        int pendingBefore = jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE kind = 'MEMBER_ADDED' AND status = 'PENDING'", Integer.class);
        String targetEmail = "invited_" + System.currentTimeMillis() + "@gmail.com";
        User recipient = createVerifiedUser(targetEmail);
        WorkspaceInvitationCreateRequest request = new WorkspaceInvitationCreateRequest(targetEmail, WorkspaceRole.MEMBER);

        WorkspaceInvitationResponse created = transactionTemplate.execute(status ->
                invitationService.createInvitation(workspace.getId(), request)
        );
        assertNotNull(created);
        assertEquals(com.tiendev.task_management_api.model.enums.WorkspaceInvitationStatus.ADDED, created.getStatus());
        assertTrue(memberRepository.existsByWorkspaceIdAndUserId(workspace.getId(), recipient.getId()));
        Notification notice = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(recipient.getId()).stream()
                .filter(item -> item.getType() == NotificationType.WORKSPACE_MEMBER_ADDED)
                .findFirst().orElseThrow();
        assertTrue(notice.getContent().contains("đã được thêm"));

        assertEquals(pendingBefore + 1, jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE kind = 'MEMBER_ADDED' AND status = 'PENDING'", Integer.class));
        verify(mailSender, never()).send(any(MimeMessage.class));

        WorkspaceInvitationResponse resent = transactionTemplate.execute(status ->
                invitationService.resendInvitation(created.getId())
        );
        assertNotNull(resent);
        assertEquals(com.tiendev.task_management_api.model.enums.WorkspaceInvitationStatus.ADDED, resent.getStatus());
        assertEquals(pendingBefore + 2, jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox WHERE kind = 'MEMBER_ADDED' AND status = 'PENDING'", Integer.class));
        mailOutbox.dispatch();
        verify(mailSender, atLeastOnce()).send(any(MimeMessage.class));
    }

    @Test
    void rolledBackInvitationDoesNotSendEmail() {
        clearInvocations(mailSender);
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox", Integer.class);
        String targetEmail = "rollback_" + System.currentTimeMillis() + "@example.com";
        createVerifiedUser(targetEmail);
        transactionTemplate.execute(status -> {
            invitationService.createInvitation(workspace.getId(),
                    new WorkspaceInvitationCreateRequest(targetEmail, WorkspaceRole.MEMBER));
            status.setRollbackOnly();
            return null;
        });

        verify(mailSender, never()).send(any(MimeMessage.class));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox", Integer.class));
    }

    @Test
    void failedDeliveryStaysQueuedForRetry() {
        String targetEmail = "retry_" + System.nanoTime() + "@example.com";
        createVerifiedUser(targetEmail);
        transactionTemplate.execute(status -> {
            invitationService.createInvitation(workspace.getId(),
                    new WorkspaceInvitationCreateRequest(targetEmail, WorkspaceRole.MEMBER));
            return null;
        });
        Long id = jdbc.queryForObject("SELECT MAX(id) FROM mail_outbox", Long.class);
        doThrow(new MailSendException("offline")).when(mailSender).send(any(MimeMessage.class));

        mailOutbox.dispatch();

        assertEquals("PENDING", jdbc.queryForObject("SELECT status FROM mail_outbox WHERE id = ?", String.class, id));
        assertEquals(1, jdbc.queryForObject("SELECT attempts FROM mail_outbox WHERE id = ?", Integer.class, id));
    }

    @Test
    void incorrectOtpAttemptIsCommitted() {
        User user = new User();
        user.setUsername("otp_" + System.currentTimeMillis());
        user.setEmail("otp_" + System.currentTimeMillis() + "@example.com");
        user.setPassword("password123");
        user.setRole(Role.USER);
        user.setEmailVerified(false);
        user = userRepository.save(user);

        EmailVerificationCode code = new EmailVerificationCode();
        code.setUser(user);
        code.setCodeHash(passwordEncoder.encode("123456"));
        code.setExpiresAt(java.time.LocalDateTime.now().plusMinutes(5));
        verificationCodeRepository.save(code);
        Long codeId = code.getId();
        String email = user.getEmail();

        assertThrows(InvalidOtpException.class, () -> authService.verifyEmail(
                new VerifyEmailRequest(email, "000000")));

        assertEquals(1, verificationCodeRepository.findById(codeId).orElseThrow().getAttemptCount());
    }

    private User createVerifiedUser(String email) {
        User user = new User();
        user.setUsername("member_" + System.nanoTime());
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(Role.USER);
        user.setEmailVerified(true);
        return userRepository.save(user);
    }
}
