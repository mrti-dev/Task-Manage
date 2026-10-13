package com.tiendev.task_management_api.event;

import com.tiendev.task_management_api.service.MailOutbox;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.event.EventListener;

@Component
@RequiredArgsConstructor
public class RegistrationOtpEventListener {

    private final MailOutbox outbox;

    @EventListener
    public void sendRegistrationOtp(RegistrationOtpEvent event) {
        outbox.enqueue(event);
    }
}
