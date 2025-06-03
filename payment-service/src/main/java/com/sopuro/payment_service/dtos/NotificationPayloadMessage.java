package com.sopuro.payment_service.dtos;

import com.sopuro.payment_service.enums.NotificationType;
import com.sopuro.payment_service.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationPayloadMessage {
    private UUID transactionId;
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
