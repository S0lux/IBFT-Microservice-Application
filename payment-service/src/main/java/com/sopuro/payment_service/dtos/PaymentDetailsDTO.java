package com.sopuro.payment_service.dtos;

import com.sopuro.payment_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class PaymentDetailsDTO {
    private String recipientAccountNumber;
    private PaymentStatus status;
    private BigDecimal senderFinalBalance;
    private BigDecimal recipientFinalBalance;
    private BigDecimal amount;
    private BigDecimal fee;
    private String failureReason;
    private String description;
    private Instant createdAt;
}
