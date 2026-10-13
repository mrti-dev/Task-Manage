package com.tiendev.task_management_api.integration;

import com.tiendev.task_management_api.event.RegistrationOtpEvent;
import com.tiendev.task_management_api.service.EmailService;
import com.tiendev.task_management_api.service.MailOutbox;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(properties = "app.mail.outbox-immediate=true")
@ActiveProfiles("test")
class MailOutboxImmediateDispatchTest {
    @Autowired private MailOutbox outbox;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;
    @MockBean private EmailService emailService;

    @Test
    void committedMailStartsInBackgroundWithoutWaitingForSmtp() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(emailService).sendRegistrationOtp(anyString(), anyString(), anyInt());

        transactions.execute(status -> {
            outbox.enqueue(new RegistrationOtpEvent("fast@example.com", "123456", 5));
            return null;
        });

        try {
            assertTrue(started.await(2, TimeUnit.SECONDS));
            assertEquals("PROCESSING", jdbc.queryForObject("SELECT status FROM mail_outbox ORDER BY id DESC LIMIT 1", String.class));
        } finally {
            release.countDown();
        }
        for (int i = 0; i < 50; i++) {
            if ("SENT".equals(jdbc.queryForObject("SELECT status FROM mail_outbox ORDER BY id DESC LIMIT 1", String.class))) return;
            Thread.sleep(20);
        }
        fail("Mail was not marked sent");
    }

    @Test
    void rolledBackMailNeverStarts() throws Exception {
        int before = jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox", Integer.class);
        transactions.execute(status -> {
            outbox.enqueue(new RegistrationOtpEvent("rollback@example.com", "654321", 5));
            status.setRollbackOnly();
            return null;
        });

        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM mail_outbox", Integer.class));
        verifyNoInteractions(emailService);
    }

    @Test
    void separateMailsStartWithoutWaitingForEachOther() throws Exception {
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            started.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(emailService).sendRegistrationOtp(anyString(), anyString(), anyInt());

        try {
            transactions.execute(status -> {
                outbox.enqueue(new RegistrationOtpEvent("one@example.com", "111111", 5));
                return null;
            });
            transactions.execute(status -> {
                outbox.enqueue(new RegistrationOtpEvent("two@example.com", "222222", 5));
                return null;
            });
            assertTrue(started.await(2, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }
    }
}
