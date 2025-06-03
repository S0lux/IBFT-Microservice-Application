package com.sopuro.notification_service.sender.impl;

import com.sopuro.notification_service.dtos.NotificationPayloadMessage;
import com.sopuro.notification_service.enums.NotificationType;
import com.sopuro.notification_service.sender.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    @Override
    public void sendNotification(NotificationPayloadMessage payload) throws MailException {
        if (payload.getRecipientEmail() == null || payload.getRecipientEmail().isEmpty()) {
            log.warn("Recipient email is missing for transactionId: {}. Skipping email notification.", payload.getTransactionId());
            return;
        }

        log.info("Preparing email notification for transactionId: {} to email: {}",
                payload.getTransactionId(), payload.getRecipientEmail());

        SimpleMailMessage message = getSimpleMailMessage(payload);

        try {
            mailSender.send(message);
            log.info("Email notification sent successfully for transactionId: {} to {}",
                    payload.getTransactionId(), payload.getRecipientEmail());
        } catch (MailException e) {
            log.error("Failed to send email for transactionId: {} to {}. Error: {}",
                    payload.getTransactionId(), payload.getRecipientEmail(), e.getMessage());
            throw e;
        }
    }

    private SimpleMailMessage getSimpleMailMessage(NotificationPayloadMessage payload) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(payload.getRecipientEmail());
        message.setSubject("IBFT Transaction Notification: " + payload.getStatus());

        // Construct email body
        String emailBody = String.format(
                """
                        Dear Customer,
                        
                        This is a notification regarding your IBFT transaction.
                        
                        Transaction ID: %s
                        Status: %s
                        Amount: %s
                        Sender: %s (%s)
                        Receiver: %s (%s)
                        Timestamp: %s
                        Details: %s
                        
                        Thank you.""",
                payload.getTransactionId(),
                payload.getStatus(),
                payload.getAmount(),
                payload.getSenderName(), payload.getSenderAccount(),
                payload.getReceiverName(), payload.getReceiverAccount(),
                payload.getTimestamp(),
                payload.getMessageDetails() != null ? payload.getMessageDetails() : "N/A"
        );
        message.setText(emailBody);
        return message;
    }

    @Override
    public NotificationType getNotificationType() {
        return NotificationType.EMAIL;
    }
}