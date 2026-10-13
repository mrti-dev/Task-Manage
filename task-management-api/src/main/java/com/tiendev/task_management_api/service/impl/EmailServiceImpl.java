package com.tiendev.task_management_api.service.impl;

import com.tiendev.task_management_api.model.enums.WorkspaceRole;
import com.tiendev.task_management_api.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:${spring.mail.username:no-reply@taskmanagement.com}}")
    private String fromEmail;

    @Override
    public void sendRegistrationOtp(String toEmail, String otp, int expirationMinutes) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Task Manager] Mã xác minh tài khoản");

            String htmlContent = """
                    <div style="font-family: Arial, sans-serif; max-width: 560px; margin: 0 auto; padding: 24px; border: 1px solid #E5E7EB; borderRadius: 8px;">
                        <h2 style="color: #0F766E; margin-bottom: 16px;">Xác minh địa chỉ email của bạn</h2>
                        <p style="color: #374151; font-size: 15px; line-height: 1.5;">Xin chào,</p>
                        <p style="color: #374151; font-size: 15px; line-height: 1.5;">Cảm ơn bạn đã đăng ký tài khoản tại Task Manager. Mã xác minh (OTP) của bạn là:</p>
                        <div style="text-align: center; margin: 24px 0;">
                            <span style="display: inline-block; padding: 12px 28px; font-size: 28px; font-weight: bold; letter-spacing: 6px; color: #0F766E; background-color: #F0FDFA; border: 1px dashed #0F766E; border-radius: 8px;">
                                %s
                            </span>
                        </div>
                        <p style="color: #6B7280; font-size: 14px;">Mã này có hiệu lực trong <strong>%d phút</strong>. Vui lòng không chia sẻ mã này cho bất kỳ ai.</p>
                        <hr style="border: none; border-top: 1px solid #E5E7EB; margin: 24px 0;" />
                        <p style="color: #9CA3AF; font-size: 12px; margin: 0;">Nếu bạn không thực hiện yêu cầu này, vui lòng bỏ qua email.</p>
                    </div>
                    """.formatted(otp, expirationMinutes);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to prepare OTP email", e);
        }
    }

    @Override
    public void sendWorkspaceMemberAdded(String toEmail, String inviterName, String workspaceName,
                                         WorkspaceRole role, String workspaceUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Task Manager] Bạn đã được thêm vào workspace: " + workspaceName);
            String html = """
                    <div style="font-family:Arial,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E5E7EB;border-radius:8px">
                      <h2 style="color:#0F766E">Bạn đã được thêm vào workspace</h2>
                      <p>Xin chào, <strong>%s</strong> đã thêm bạn vào workspace <strong>\"%s\"</strong> với vai trò <strong>%s</strong>.</p>
                      <p><a href="%s" style="display:inline-block;padding:12px 24px;color:#fff;background:#0F766E;text-decoration:none;border-radius:6px">Mở workspace</a></p>
                      <p style="color:#6B7280">Nếu bạn không nhận ra thay đổi này, hãy liên hệ người quản lý workspace.</p>
                    </div>
                    """.formatted(inviterName, workspaceName, role, workspaceUrl);
            helper.setText(html, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Failed to prepare workspace notification email", e);
        }
    }
}
