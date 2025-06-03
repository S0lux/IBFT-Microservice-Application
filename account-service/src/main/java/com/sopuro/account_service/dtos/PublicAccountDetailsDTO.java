package com.sopuro.account_service.dtos;

import com.sopuro.account_service.enums.AccountStatus;
import com.sopuro.account_service.enums.AccountType;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicAccountDetailsDTO {
    private String number;
    private AccountType type;
    private AccountStatus status;
    private UserDetailsDTO owner;
}
