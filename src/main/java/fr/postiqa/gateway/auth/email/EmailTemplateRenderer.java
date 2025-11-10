package fr.postiqa.gateway.auth.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Renders email templates with dynamic content.
 * Uses simple HTML strings for professional email design.
 */
@Component
public class EmailTemplateRenderer {

    @Value("${mail.from.name}")
    private String brandName;

    /**
     * Render email template with variables
     */
    public String render(EmailTemplate template, Map<String, String> variables) {
        return switch (template) {
            case WELCOME -> renderWelcomeEmail(variables);
            case EMAIL_VERIFICATION -> renderEmailVerification(variables);
            case PASSWORD_RESET -> renderPasswordReset(variables);
            case PASSWORD_CHANGED -> renderPasswordChanged(variables);
        };
    }

    private String renderWelcomeEmail(Map<String, String> vars) {
        String firstName = vars.get("firstName");
        String verificationUrl = vars.get("verificationUrl");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">Welcome to %s!</h1>
                </div>

                <div style="background: #ffffff; padding: 40px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <p style="font-size: 16px; margin-bottom: 20px;">Hi %s,</p>

                    <p style="font-size: 16px; margin-bottom: 20px;">
                        Thank you for joining %s! We're excited to have you on board.
                    </p>

                    <p style="font-size: 16px; margin-bottom: 30px;">
                        To get started, please verify your email address by clicking the button below:
                    </p>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 40px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>

                    <p style="font-size: 14px; color: #666; margin-top: 30px;">
                        If the button doesn't work, copy and paste this link into your browser:
                    </p>
                    <p style="font-size: 14px; color: #667eea; word-break: break-all;">
                        %s
                    </p>

                    <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        This link will expire in 24 hours for security reasons.
                    </p>

                    <p style="font-size: 16px; margin-top: 30px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>

                <div style="text-align: center; margin-top: 20px; color: #999; font-size: 12px;">
                    <p>If you didn't create an account, please ignore this email.</p>
                </div>
            </body>
            </html>
            """.formatted(brandName, firstName, brandName, verificationUrl, verificationUrl, brandName);
    }

    private String renderEmailVerification(Map<String, String> vars) {
        String firstName = vars.get("firstName");
        String verificationUrl = vars.get("verificationUrl");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">Verify Your Email</h1>
                </div>

                <div style="background: #ffffff; padding: 40px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <p style="font-size: 16px; margin-bottom: 20px;">Hi %s,</p>

                    <p style="font-size: 16px; margin-bottom: 30px;">
                        Please verify your email address to complete your %s account setup.
                    </p>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 14px 40px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>

                    <p style="font-size: 14px; color: #666; margin-top: 30px;">
                        If the button doesn't work, copy and paste this link into your browser:
                    </p>
                    <p style="font-size: 14px; color: #667eea; word-break: break-all;">
                        %s
                    </p>

                    <p style="font-size: 14px; color: #666; margin-top: 30px; padding-top: 20px; border-top: 1px solid #eee;">
                        This link will expire in 24 hours.
                    </p>

                    <p style="font-size: 16px; margin-top: 30px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>

                <div style="text-align: center; margin-top: 20px; color: #999; font-size: 12px;">
                    <p>If you didn't request this email, please ignore it.</p>
                </div>
            </body>
            </html>
            """.formatted(firstName, brandName, verificationUrl, verificationUrl, brandName);
    }

    private String renderPasswordReset(Map<String, String> vars) {
        String firstName = vars.get("firstName");
        String resetUrl = vars.get("resetUrl");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">Reset Your Password</h1>
                </div>

                <div style="background: #ffffff; padding: 40px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <p style="font-size: 16px; margin-bottom: 20px;">Hi %s,</p>

                    <p style="font-size: 16px; margin-bottom: 20px;">
                        We received a request to reset your password for your %s account.
                    </p>

                    <p style="font-size: 16px; margin-bottom: 30px;">
                        Click the button below to create a new password:
                    </p>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="%s" style="background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; padding: 14px 40px; text-decoration: none; border-radius: 5px; font-weight: bold; font-size: 16px; display: inline-block;">
                            Reset Password
                        </a>
                    </div>

                    <p style="font-size: 14px; color: #666; margin-top: 30px;">
                        If the button doesn't work, copy and paste this link into your browser:
                    </p>
                    <p style="font-size: 14px; color: #f5576c; word-break: break-all;">
                        %s
                    </p>

                    <div style="background: #fff3cd; border: 1px solid #ffc107; border-radius: 5px; padding: 15px; margin-top: 30px;">
                        <p style="font-size: 14px; color: #856404; margin: 0;">
                            <strong>Security Notice:</strong> This link will expire in 1 hour. If you didn't request a password reset, please ignore this email and ensure your account is secure.
                        </p>
                    </div>

                    <p style="font-size: 16px; margin-top: 30px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>

                <div style="text-align: center; margin-top: 20px; color: #999; font-size: 12px;">
                    <p>If you didn't request this password reset, someone may be trying to access your account. Please secure your account immediately.</p>
                </div>
            </body>
            </html>
            """.formatted(firstName, brandName, resetUrl, resetUrl, brandName);
    }

    private String renderPasswordChanged(Map<String, String> vars) {
        String firstName = vars.get("firstName");

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; max-width: 600px; margin: 0 auto; padding: 20px;">
                <div style="background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); padding: 30px; text-align: center; border-radius: 10px 10px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px;">Password Changed Successfully</h1>
                </div>

                <div style="background: #ffffff; padding: 40px; border-radius: 0 0 10px 10px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                    <p style="font-size: 16px; margin-bottom: 20px;">Hi %s,</p>

                    <p style="font-size: 16px; margin-bottom: 20px;">
                        Your password for your %s account has been successfully changed.
                    </p>

                    <div style="background: #d4edda; border: 1px solid #28a745; border-radius: 5px; padding: 15px; margin: 20px 0;">
                        <p style="font-size: 14px; color: #155724; margin: 0;">
                            <strong>Confirmation:</strong> Your password was updated successfully. You can now use your new password to log in.
                        </p>
                    </div>

                    <p style="font-size: 16px; margin-top: 30px;">
                        If you didn't make this change or if you believe an unauthorized person has accessed your account, please contact our support team immediately.
                    </p>

                    <p style="font-size: 16px; margin-top: 30px;">
                        Best regards,<br>
                        The %s Team
                    </p>
                </div>

                <div style="text-align: center; margin-top: 20px; color: #999; font-size: 12px;">
                    <p>This is an automated security notification.</p>
                </div>
            </body>
            </html>
            """.formatted(firstName, brandName, brandName);
    }
}
