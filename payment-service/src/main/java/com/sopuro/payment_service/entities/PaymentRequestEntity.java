package com.sopuro.payment_service.entities;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.UUID;

@Document(collection = "payment_requests")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequestEntity {
    @Id
    private UUID paymentId;
    private String senderAccountNumber;
    private String recipientAccountNumber;
    private BigDecimal amount;
    private BigDecimal fee;
    private Boolean isSent;
}
