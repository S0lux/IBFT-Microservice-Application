package com.sopuro.payment_service.dtos;

import com.sopuro.payment_service.enums.PaymentStatus;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedPaymentMessageDTO {
    private UUID paymentId;
    private PaymentStatus status;
    private String failureReason;
}
