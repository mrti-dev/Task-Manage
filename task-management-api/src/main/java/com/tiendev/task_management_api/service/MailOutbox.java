package com.tiendev.task_management_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiendev.task_management_api.event.RegistrationOtpEvent;
import com.tiendev.task_management_api.event.WorkspaceMemberAddedEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class MailOutbox {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final EmailService emailService;
    private final TaskExecutor executor;
    private final SecureRandom random = new SecureRandom();

    @Value("${app.mail.outbox-key}")
    private String outboxKey;
    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;
    @Value("${app.mail.outbox-immediate:true}")
    private boolean immediateDispatch;
    private SecretKeySpec key;

    public MailOutbox(JdbcTemplate jdbc, ObjectMapper mapper, EmailService emailService,
                      @Qualifier("mailOutboxExecutor") TaskExecutor executor) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.emailService = emailService;
        this.executor = executor;
    }

    @PostConstruct
    void initialize() throws Exception {
        if (outboxKey.length() < 32) throw new IllegalStateException("MAIL_OUTBOX_KEY must contain at least 32 characters");
        key = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(outboxKey.getBytes(StandardCharsets.UTF_8)), "AES");
    }

    public void enqueue(RegistrationOtpEvent event) {
        enqueue("OTP", event);
    }

    public void enqueue(WorkspaceMemberAddedEvent event) {
        enqueue("MEMBER_ADDED", event);
    }

    private void enqueue(String kind, Object event) {
        try {
            String payload = encrypt(mapper.writeValueAsBytes(event));
            LocalDateTime now = LocalDateTime.now();
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                var statement = connection.prepareStatement(
                        "INSERT INTO mail_outbox (kind, payload, status, next_attempt_at, created_at) VALUES (?, ?, 'PENDING', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS);
                statement.setString(1, kind);
                statement.setString(2, payload);
                statement.setObject(3, now);
                statement.setObject(4, now);
                return statement;
            }, keys);
            if (immediateDispatch) {
                long id = Objects.requireNonNull(keys.getKey()).longValue();
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            wake(id);
                        }
                    });
                } else {
                    wake(id);
                }
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cannot serialize mail", e);
        }
    }

    private void wake(long id) {
        try {
            executor.execute(() -> deliver(id));
        } catch (TaskRejectedException e) {
            log.warn("Mail wakeup deferred to polling: id={}", id);
        }
    }

    @Scheduled(fixedDelayString = "${app.mail.outbox-poll-ms:1000}", initialDelayString = "${app.mail.outbox-poll-ms:1000}")
    public void dispatch() {
        LocalDateTime now = LocalDateTime.now();
        List<Long> ids = jdbc.queryForList("SELECT id FROM mail_outbox WHERE (status = 'PENDING' AND next_attempt_at <= ?) OR (status = 'PROCESSING' AND claimed_until <= ?) ORDER BY CASE WHEN kind = 'OTP' THEN 0 ELSE 1 END, id LIMIT 20", Long.class, now, now);
        for (Long id : ids) {
            deliver(id);
        }
    }

    private void deliver(long id) {
        LocalDateTime now = LocalDateTime.now();
        if (jdbc.update("UPDATE mail_outbox SET status = 'PROCESSING', claimed_until = ? WHERE id = ? AND ((status = 'PENDING' AND next_attempt_at <= ?) OR (status = 'PROCESSING' AND claimed_until <= ?))",
                now.plusMinutes(2), id, now, now) != 1) return;
        try {
            var rows = jdbc.query("SELECT kind, payload, created_at FROM mail_outbox WHERE id = ?",
                    (rs, rowNum) -> new QueuedMail(rs.getString(1), rs.getString(2), rs.getTimestamp(3).toLocalDateTime()), id);
            QueuedMail row = rows.get(0);
            byte[] payload = decrypt(row.payload());
            long started = System.nanoTime();
            if ("OTP".equals(row.kind())) {
                var event = mapper.readValue(payload, RegistrationOtpEvent.class);
                emailService.sendRegistrationOtp(event.email(), event.otp(), event.expirationMinutes());
            } else if ("MEMBER_ADDED".equals(row.kind())) {
                var event = mapper.readValue(payload, WorkspaceMemberAddedEvent.class);
                String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
                emailService.sendWorkspaceMemberAdded(event.toEmail(), event.inviterName(), event.workspaceName(),
                        event.role(), baseUrl + "/workspaces/" + event.workspaceId());
            } else throw new IllegalStateException("Unknown mail kind");
            jdbc.update("UPDATE mail_outbox SET status = 'SENT', payload = '', claimed_until = NULL WHERE id = ?", id);
            log.info("Mail delivered: id={}, kind={}, queue_ms={}, smtp_ms={}", id, row.kind(),
                    Math.max(0, Duration.between(row.createdAt(), LocalDateTime.now()).toMillis()),
                    Duration.ofNanos(System.nanoTime() - started).toMillis());
        } catch (Exception e) {
            Integer attempts = jdbc.queryForObject("SELECT attempts FROM mail_outbox WHERE id = ?", Integer.class, id);
            int count = attempts == null ? 1 : attempts + 1;
            jdbc.update("UPDATE mail_outbox SET status = ?, attempts = ?, next_attempt_at = ?, claimed_until = NULL WHERE id = ?",
                    count >= 5 ? "FAILED" : "PENDING", count, LocalDateTime.now().plusSeconds(1L << Math.min(count, 6)), id);
            log.error("Mail outbox delivery failed: id={}, attempt={}, error={}", id, count, e.getClass().getSimpleName());
        }
    }

    private record QueuedMail(String kind, String payload, LocalDateTime createdAt) {}

    private String encrypt(byte[] data) {
        try {
            byte[] iv = new byte[12];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(data);
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot encrypt mail", e);
        }
    }

    private byte[] decrypt(String value) throws Exception {
        byte[] combined = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, combined, 0, 12));
        return cipher.doFinal(combined, 12, combined.length - 12);
    }
}
