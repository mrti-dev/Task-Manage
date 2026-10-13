package com.tiendev.task_management_api.event;

public record RegistrationOtpEvent(String email, String otp, int expirationMinutes) {
}
