package com.restaurant.notifications.service;

import com.restaurant.notifications.kafka.OrderReadyEvent;
import com.restaurant.notifications.kafka.ReservationEvent;
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

    @Value("${app.frontend.url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReservationConfirmed(ReservationEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(event.customerEmail());
            helper.setSubject("🍽️ Reservation Confirmed - Odin Restaurant");
            helper.setText(buildReservationConfirmedTemplate(event), true);
            mailSender.send(message);
            log.info("Reservation confirmed email sent to {}", event.customerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send reservation confirmed email to {}", event.customerEmail(), e);
        }
    }

    public void sendReservationCancelled(ReservationEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(event.customerEmail());
            helper.setSubject("Reservation Cancelled - Odin Restaurant");
            helper.setText(buildReservationCancelledTemplate(event), true);
            mailSender.send(message);
            log.info("Reservation cancelled email sent to {}", event.customerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send reservation cancelled email to {}", event.customerEmail(), e);
        }
    }

    public void sendOrderReady(OrderReadyEvent event) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(event.customerEmail());
            helper.setSubject("🍴 Your Order is Ready - Odin Restaurant");
            helper.setText(buildOrderReadyTemplate(event), true);
            mailSender.send(message);
            log.info("Order ready email sent to {}", event.customerEmail());
        } catch (MessagingException e) {
            log.error("Failed to send order ready email to {}", event.customerEmail(), e);
        }
    }

    private String buildReservationConfirmedTemplate(ReservationEvent event) {
        String myReservationsUrl = frontendUrl + "/my-reservations";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #8bc395 0%%, #7db88a 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 600; }
                    .content { padding: 40px 30px; background: #fdfaf6; }
                    .content h2 { color: #2d5f3f; margin-top: 0; }
                    .content p { color: #3d7050; font-size: 16px; margin: 15px 0; }
                    .details-box { background: white; border: 2px solid #e8f5ea; border-radius: 10px; padding: 20px; margin: 20px 0; }
                    .details-box p { margin: 8px 0; color: #333; }
                    .details-box strong { color: #2d5f3f; }
                    .button-container { text-align: center; margin: 30px 0; }
                    .button { display: inline-block; padding: 16px 40px; background: linear-gradient(135deg, #8bc395 0%%, #7db88a 100%%); color: white !important; text-decoration: none; border-radius: 30px; font-weight: 600; font-size: 16px; }
                    .warning { background: rgba(224,142,168,0.1); border-left: 4px solid #e08ea8; padding: 15px; border-radius: 5px; margin: 20px 0; }
                    .warning p { margin: 0; color: #c96c8a; font-size: 14px; }
                    .footer { text-align: center; padding: 30px; background: #2d5f3f; color: white; }
                    .footer p { margin: 5px 0; font-size: 14px; color: #e8f5ea; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>🍽️ Odin Restaurant</h1></div>
                    <div class="content">
                        <h2>Your Reservation is Confirmed!</h2>
                        <p>Hi %s! 👋</p>
                        <p>Great news — your reservation has been confirmed. Here are your details:</p>
                        <div class="details-box">
                            <p><strong>📅 Date:</strong> %s</p>
                            <p><strong>🕐 Time:</strong> %s – %s</p>
                            <p><strong>👥 Party size:</strong> %d person(s)</p>
                        </div>
                        <div class="warning">
                            <p><strong>⚠️ Cancellation policy:</strong> Please cancel at least 24 hours in advance if you cannot make it.</p>
                        </div>
                        <p>Need to cancel? You can manage your reservation from your account:</p>
                        <div class="button-container">
                            <a href="%s" class="button">View My Reservations</a>
                        </div>
                    </div>
                    <div class="footer"><p>© 2025 Odin Restaurant. All rights reserved.</p><p>Made with ❤️ for food lovers</p></div>
                </div>
            </body>
            </html>
            """.formatted(
                event.customerName(),
                event.reservationDate(),
                event.startTime(),
                event.endTime(),
                event.partySize(),
                myReservationsUrl
            );
    }

    private String buildReservationCancelledTemplate(ReservationEvent event) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #e08ea8 0%%, #c96c8a 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 600; }
                    .content { padding: 40px 30px; background: #fdfaf6; }
                    .content h2 { color: #c96c8a; margin-top: 0; }
                    .content p { color: #555; font-size: 16px; margin: 15px 0; }
                    .details-box { background: white; border: 2px solid #fce8ee; border-radius: 10px; padding: 20px; margin: 20px 0; }
                    .details-box p { margin: 8px 0; color: #333; }
                    .details-box strong { color: #c96c8a; }
                    .footer { text-align: center; padding: 30px; background: #2d5f3f; color: white; }
                    .footer p { margin: 5px 0; font-size: 14px; color: #e8f5ea; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>🍽️ Odin Restaurant</h1></div>
                    <div class="content">
                        <h2>Reservation Cancelled</h2>
                        <p>Hi %s,</p>
                        <p>Your reservation has been cancelled. Here were the details:</p>
                        <div class="details-box">
                            <p><strong>📅 Date:</strong> %s</p>
                            <p><strong>🕐 Time:</strong> %s – %s</p>
                            <p><strong>👥 Party size:</strong> %d person(s)</p>
                            %s
                        </div>
                        <p>We hope to see you again soon. Feel free to make a new reservation anytime.</p>
                    </div>
                    <div class="footer"><p>© 2025 Odin Restaurant. All rights reserved.</p><p>Made with ❤️ for food lovers</p></div>
                </div>
            </body>
            </html>
            """.formatted(
                event.customerName(),
                event.reservationDate(),
                event.startTime(),
                event.endTime(),
                event.partySize(),
                event.cancelReason() != null
                    ? "<p><strong>📝 Reason:</strong> " + event.cancelReason() + "</p>"
                    : ""
            );
    }

    private String buildOrderReadyTemplate(OrderReadyEvent event) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; background-color: #f5f5f5; }
                    .container { max-width: 600px; margin: 40px auto; background: white; border-radius: 15px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #8bc395 0%%, #7db88a 100%%); color: white; padding: 40px 30px; text-align: center; }
                    .header h1 { margin: 0; font-size: 28px; font-weight: 600; }
                    .content { padding: 40px 30px; background: #fdfaf6; }
                    .content h2 { color: #2d5f3f; margin-top: 0; }
                    .content p { color: #3d7050; font-size: 16px; margin: 15px 0; }
                    .highlight { font-size: 48px; text-align: center; margin: 20px 0; }
                    .footer { text-align: center; padding: 30px; background: #2d5f3f; color: white; }
                    .footer p { margin: 5px 0; font-size: 14px; color: #e8f5ea; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header"><h1>🍽️ Odin Restaurant</h1></div>
                    <div class="content">
                        <h2>Your Order is Ready! 🎉</h2>
                        <div class="highlight">🍴</div>
                        <p>Great news! Your order <strong>#%d</strong> is now ready.</p>
                        <p>Please proceed to pick it up. Enjoy your meal!</p>
                    </div>
                    <div class="footer"><p>© 2025 Odin Restaurant. All rights reserved.</p><p>Made with ❤️ for food lovers</p></div>
                </div>
            </body>
            </html>
            """.formatted(event.orderId());
    }
}
