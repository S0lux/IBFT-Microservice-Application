package com.sopuro.account_service.services;

import com.sopuro.account_service.dtos.PaymentRequestMessageDTO;
import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.dtos.WithholdBalanceResponseDTO;
import com.sopuro.account_service.exceptions.account.AccountNotFound;
import com.sopuro.account_service.exceptions.account.InsufficientBalance;

import java.math.BigDecimal;

public interface AccountAdminService {
    void processPayment(PaymentRequestMessageDTO message) throws InsufficientBalance, AccountNotFound;
    WithholdBalanceResponseDTO withholdAvailableBalance(String accountNumber, BigDecimal amount) throws AccountNotFound, InsufficientBalance;
    void releaseWithheldBalance(String accountNumber, BigDecimal amount) throws AccountNotFound, InsufficientBalance;
    PrivateAccountDetailsDTO getAccountDetails(String accountNumber) throws AccountNotFound;
}
