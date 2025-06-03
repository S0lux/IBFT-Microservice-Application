package com.sopuro.payment_service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sopuro.payment_service.dtos.PaymentDetailsDTO;
import com.sopuro.payment_service.dtos.PaymentRequestDTO;
import com.sopuro.payment_service.services.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentPublicController {
    private final PaymentService paymentService;

    @PostMapping
    public CompletableFuture<PaymentDetailsDTO> newPayment(
            @RequestHeader("x-user-id") UUID userId,
            @RequestBody @Valid PaymentRequestDTO paymentRequestDTO
    ) {
        return paymentService.makePayment(userId, paymentRequestDTO);
    }

    @GetMapping
    public Page<PaymentDetailsDTO> getPaymentHistory(
            @RequestHeader("x-user-id") UUID userId,
            @RequestParam(defaultValue = "0") @Min(0) int pageNumber,
            @RequestParam(defaultValue = "10") @Min(5) @Max(20) int pageSize
    ) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        return paymentService.getPaymentHistory(userId, pageable);
    }
}
