package com.sopuro.account_service.entities;

import com.sopuro.account_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "processed_payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedPaymentEntity {
    @Id
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "failure_reason", nullable = true)
    private String failureReason;

    @Column(name = "is_sent", nullable = false)
    @Builder.Default
    private Boolean isSent = false;
}
