package com.sopuro.notification_service.dtos;

import com.sopuro.notification_service.enums.NotificationType;
import com.sopuro.notification_service.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPayloadMessage {
    private String transactionId;
    private String senderName;
    private String senderAccount;
    private String receiverName;
    private String receiverAccount;
    private PaymentStatus status;
    private Instant timestamp;
    private String messageDetails;
    private BigDecimal amount;

    // --- Fields for specific notification channels ---
    private String recipientEmail;
    private String fcmToken;

    private List<NotificationType> targetChannels;
}
