package com.restaurant.Apollo.Auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.verification.url}")
    private String verificationBaseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationEmail(String toEmail, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("🍽️ Verify Your Email - Odin Restaurant");

            String verificationUrl = verificationBaseUrl + "?token=" + token;
            String htmlContent = buildEmailTemplate(verificationUrl, toEmail);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            
            log.info("Verification email sent to: {}", toEmail);
        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    private String buildEmailTemplate(String verificationUrl, String email) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <style>
                        body { 
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; 
                            line-height: 1.6; 
                            color: #333; 
                            margin: 0;
                            padding: 0;
                            background-color: #f5f5f5;
                        }
                        .container { 
                            max-width: 600px; 
                            margin: 40px auto; 
                            background: white;
                            border-radius: 15px;
                            overflow: hidden;
                            box-shadow: 0 4px 20px rgba(0,0,0,0.1);
                        }
                        .header { 
                            background: linear-gradient(135deg, #8bc395 0%%, #7db88a 100%%); 
                            color: white; 
                            padding: 40px 30px; 
                            text-align: center;
                        }
                        .header h1 {
                            margin: 0;
                            font-size: 28px;
                            font-weight: 600;
                        }
                        .content { 
                            padding: 40px 30px;
                            background: #fdfaf6;
                        }
                        .content h2 {
                            color: #2d5f3f;
                            margin-top: 0;
                            font-size: 24px;
                        }
                        .content p {
                            color: #3d7050;
                            font-size: 16px;
                            margin: 15px 0;
                        }
                        .button-container {
                            text-align: center;
                            margin: 30px 0;
                        }
                        .button { 
                            display: inline-block; 
                            padding: 16px 40px; 
                            background: linear-gradient(135deg, #8bc395 0%%, #7db88a 100%%);
                            color: white !important; 
                            text-decoration: none; 
                            border-radius: 30px;
                            font-weight: 600;
                            font-size: 16px;
                            box-shadow: 0 4px 15px rgba(139, 195, 149, 0.4);
                            transition: all 0.3s;
                        }
                        .button:hover {
                            transform: translateY(-2px);
                            box-shadow: 0 6px 20px rgba(139, 195, 149, 0.5);
                        }
                        .link-box {
                            background: white;
                            padding: 15px;
                            border-radius: 10px;
                            margin: 20px 0;
                            border: 2px solid #e8f5ea;
                        }
                        .link-box p {
                            margin: 5px 0;
                            font-size: 14px;
                        }
                        .link-text {
                            word-break: break-all; 
                            color: #8bc395;
                            font-family: monospace;
                            font-size: 13px;
                        }
                        .warning {
                            background: rgba(224, 142, 168, 0.1);
                            border-left: 4px solid #e08ea8;
                            padding: 15px;
                            border-radius: 5px;
                            margin: 20px 0;
                        }
                        .warning p {
                            margin: 0;
                            color: #c96c8a;
                            font-size: 14px;
                        }
                        .footer { 
                            text-align: center; 
                            padding: 30px;
                            background: #2d5f3f;
                            color: white;
                        }
                        .footer p {
                            margin: 5px 0;
                            font-size: 14px;
                            color: #e8f5ea;
                        }
                        .email-highlight {
                            font-weight: 600;
                            color: #2d5f3f;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🍽️ Welcome to Odin Restaurant!</h1>
                        </div>
                        <div class="content">
                            <h2>Verify Your Email Address</h2>
                            <p>Hi there! 👋</p>
                            <p>Thank you for registering at <strong>Odin Restaurant</strong> with the email: <span class="email-highlight">%s</span></p>
                            <p>To complete your registration and start enjoying our delicious menu, please verify your email address by clicking the button below:</p>
                            
                            <div class="button-container">
                                <a href="%s" class="button">✓ Verify Email Address</a>
                            </div>
                            
                            <div class="warning">
                                <p><strong>⚠️ Important:</strong> This verification link will expire in <strong>24 hours</strong>.</p>
                            </div>
                            
                            <p>If you didn't create an account with Odin Restaurant, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>© 2025 Odin Restaurant. All rights reserved.</p>
                            <p>Made with ❤️ for food lovers</p>
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(email, verificationUrl);
    }
}
