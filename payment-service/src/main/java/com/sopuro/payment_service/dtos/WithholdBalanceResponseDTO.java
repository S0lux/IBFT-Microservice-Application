package com.sopuro.payment_service.dtos;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WithholdBalanceResponseDTO {
    private final BigDecimal availableBalance;
}
