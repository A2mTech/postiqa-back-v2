package fr.postiqa.gateway.auth.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Email Service for sending transactional emails.
 * Uses Spring Mail with async execution for non-blocking email delivery.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRenderer templateRenderer;

    @Value("${mail.from.address}")
    private String fromAddress;

    @Value("${mail.from.name}")
    private String fromName;

    /**
     * Send email asynchronously
     *
     * @param to recipient email
     * @param subject email subject
     * @param template email template
     * @param variables template variables
     */
    @Async
    public void sendEmail(String to, String subject, EmailTemplate template, Map<String, String> variables) {
        try {
            String htmlContent = templateRenderer.render(template, variables);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Email sent successfully to {} with template {}", to, template);
        } catch (MessagingException e) {
            log.error("Failed to send email to {} with template {}", to, template, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send welcome email
     */
    public void sendWelcomeEmail(String to, String firstName, String verificationUrl) {
        Map<String, String> variables = Map.of(
            "firstName", firstName,
            "verificationUrl", verificationUrl
        );

        sendEmail(to, "Welcome to Postiqa!", EmailTemplate.WELCOME, variables);
    }

    /**
     * Send email verification
     */
    public void sendEmailVerification(String to, String firstName, String verificationUrl) {
        Map<String, String> variables = Map.of(
            "firstName", firstName,
            "verificationUrl", verificationUrl
        );

        sendEmail(to, "Verify Your Email Address", EmailTemplate.EMAIL_VERIFICATION, variables);
    }

    /**
     * Send password reset email
     */
    public void sendPasswordResetEmail(String to, String firstName, String resetUrl) {
        Map<String, String> variables = Map.of(
            "firstName", firstName,
            "resetUrl", resetUrl
        );

        sendEmail(to, "Reset Your Password", EmailTemplate.PASSWORD_RESET, variables);
    }

    /**
     * Send password changed confirmation
     */
    public void sendPasswordChangedEmail(String to, String firstName) {
        Map<String, String> variables = Map.of(
            "firstName", firstName
        );

        sendEmail(to, "Password Changed Successfully", EmailTemplate.PASSWORD_CHANGED, variables);
    }
}
