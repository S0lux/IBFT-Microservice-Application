package com.sopuro.payment_service.entities;

import com.sopuro.payment_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Document(collection = "payments")
@Data
@Builder
public class PaymentEntity {
    @Id
    private UUID id;

    @Builder.Default
    private PaymentStatus status = PaymentStatus.PROCESSING;

    private String failureReason;

    private UUID senderId;

    private String senderAccountNumber;

    private BigDecimal senderFinalBalance;

    private BigDecimal recipientFinalBalance;

    private UUID recipientId;

    private String recipientAccountNumber;

    private BigDecimal amount;

    private BigDecimal fee;

    private String description;

    @Builder.Default
    private Instant createdAt = Instant.now();
}
