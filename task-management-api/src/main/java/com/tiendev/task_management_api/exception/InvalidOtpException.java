package com.tiendev.task_management_api.exception;

public class InvalidOtpException extends BusinessException {
    public InvalidOtpException() {
        super("Mã xác minh không chính xác.", "OTP_INVALID");
    }
}
