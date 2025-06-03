package com.sopuro.payment_service.dtos;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReleaseWithheldBalanceMessageDTO {
    private String accountNumber;
    private BigDecimal amount;
}
