package com.sopuro.payment_service.dtos;

import com.sopuro.payment_service.enums.AccountStatus;
import com.sopuro.payment_service.enums.AccountType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class PrivateAccountDetailsDTO {
    private String number;
    private UUID ownerId;
    private AccountType type;
    private AccountStatus status;
    private BigDecimal availableBalance;
    private BigDecimal holdingBalance;
    private String createdAt;
    private String updatedAt;
}
