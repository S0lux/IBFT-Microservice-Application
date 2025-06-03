package com.sopuro.account_service.services;

import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.dtos.PublicAccountDetailsDTO;
import com.sopuro.account_service.enums.AccountType;
import com.sopuro.account_service.enums.UserStatus;
import com.sopuro.account_service.exceptions.account.AccountNotFound;
import org.springframework.data.web.PagedModel;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountPublicService {
    PrivateAccountDetailsDTO createAccount(UUID userId, UserStatus userStatus, AccountType type, BigDecimal initialBalance)
            throws IllegalArgumentException;

    PagedModel<PrivateAccountDetailsDTO> getUserPrivateAccounts(UUID userId, int page, int size)
            throws IllegalArgumentException;

    PublicAccountDetailsDTO getPublicAccountInfo(String accountNumber)
            throws AccountNotFound, IllegalArgumentException;
}
