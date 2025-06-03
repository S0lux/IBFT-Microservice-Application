package com.sopuro.account_service.dtos;

import com.sopuro.account_service.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResponseDTO {
    private String paymentId;
    private PaymentStatus status;
    private String failureReason;
}
