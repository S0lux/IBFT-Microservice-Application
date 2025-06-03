package com.sopuro.account_service.services;

import com.sopuro.account_service.dtos.PaymentRequestMessageDTO;
import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.dtos.WithholdBalanceResponseDTO;
import com.sopuro.account_service.entities.AccountEntity;
import com.sopuro.account_service.entities.ProcessedPaymentEntity;
import com.sopuro.account_service.enums.PaymentStatus;
import com.sopuro.account_service.exceptions.account.AccountNotFound;
import com.sopuro.account_service.exceptions.account.InsufficientBalance;
import com.sopuro.account_service.mappers.AccountMapper;
import com.sopuro.account_service.repositories.AccountRepository;
import com.sopuro.account_service.repositories.ProcessedPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountAdminServiceImpl implements AccountAdminService {
    private final AccountRepository accountRepository;
    private final ProcessedPaymentRepository processedPaymentRepository;
    private final AccountMapper accountMapper;

    @Transactional("transactionManager")
    public void processPayment(PaymentRequestMessageDTO message) throws AccountNotFound {
        // Step 1: Subtract from the sender's holding balance
        int holdingBalanceSubtracted = accountRepository.subtractHoldingBalance(
                message.getSenderAccountNumber(),
                message.getAmount().abs().add(message.getFee()));

        if (holdingBalanceSubtracted == 0) {
            throw new InsufficientBalance(message.getSenderAccountNumber());
        }

        // Step 2: Add to the receiver's available balance
        int availableBalanceAdded = accountRepository.addAvailableBalance(
                message.getRecipientAccountNumber(),
                message.getAmount().abs());

        if (availableBalanceAdded == 0) {
            // Transaction will automatically roll back the sender deduction
            throw new AccountNotFound(message.getRecipientAccountNumber());
        }

        // Step 3: Save a successful payment record
        ProcessedPaymentEntity successfulPayment = ProcessedPaymentEntity.builder()
                .paymentId(message.getPaymentId())
                .status(PaymentStatus.SUCCEED)
                .build();
        processedPaymentRepository.save(successfulPayment);
    }

    @Override
    @Transactional("transactionManager")
    public WithholdBalanceResponseDTO withholdAvailableBalance(String accountNumber, BigDecimal amount)
            throws AccountNotFound, InsufficientBalance {
        Boolean accountExists = accountRepository.existsById_Number(accountNumber);
        if (!accountExists) throw new AccountNotFound(accountNumber);

        int updatedRows = accountRepository.withholdAvailableBalance(accountNumber, amount);
        if (updatedRows == 0) throw new InsufficientBalance(accountNumber);

        Optional<AccountEntity> account = accountRepository.findById_Number(accountNumber);
        if (account.isEmpty()) throw new AccountNotFound(accountNumber);

        WithholdBalanceResponseDTO response = WithholdBalanceResponseDTO.builder()
                .availableBalance(account.get().getAvailableBalance())
                .build();

        log.info("Withheld {} from account {}", amount, accountNumber);

        return response;
    }

    @Override
    @Transactional("transactionManager")
    public void releaseWithheldBalance(String accountNumber, BigDecimal amount) throws AccountNotFound, InsufficientBalance {
        Boolean accountExists = accountRepository.existsById_Number(accountNumber);
        if (!accountExists) throw new AccountNotFound(accountNumber);

        int updatedRows = accountRepository.releaseWithheldBalance(accountNumber, amount);
        if (updatedRows == 0) throw new InsufficientBalance(accountNumber);

        log.info("Released {} from account {}", amount, accountNumber);
    }

    @Override
    public PrivateAccountDetailsDTO getAccountDetails(String accountNumber) throws AccountNotFound {
        Optional<AccountEntity> account = accountRepository.findById_Number(accountNumber);
        if (account.isEmpty()) throw new AccountNotFound(accountNumber);

        return accountMapper.toAccountDetailsDTO(account.get());
    }
}
