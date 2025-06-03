package com.sopuro.payment_service.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentRequestDTO {
    @NotBlank(message = "Sender account number cannot be blank")
    private String senderAccountNumber;

    @NotBlank(message = "Recipient account number cannot be blank")
    private String recipientAccountNumber;

    private String description;

    @Min(value = 1, message = "Amount must be greater than 0")
    private BigDecimal amount;
}
