package com.sopuro.account_service.dtos;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequestMessageDTO {
    private UUID paymentId;
    private String senderAccountNumber;
    private String recipientAccountNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private Boolean isSent;
}