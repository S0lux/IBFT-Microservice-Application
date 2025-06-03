package com.sopuro.account_service.dtos;

import com.sopuro.account_service.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequestDTO {
    @NotNull(message = "Account type is required")
    private AccountType type;
}
