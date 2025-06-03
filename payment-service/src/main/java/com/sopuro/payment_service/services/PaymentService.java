package com.sopuro.payment_service.services;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sopuro.payment_service.dtos.PaymentDetailsDTO;
import com.sopuro.payment_service.dtos.PaymentRequestDTO;

public interface PaymentService {
    CompletableFuture<PaymentDetailsDTO> makePayment(UUID requesterId, PaymentRequestDTO request);
    Page<PaymentDetailsDTO> getPaymentHistory(UUID userId, Pageable pageable);
}
