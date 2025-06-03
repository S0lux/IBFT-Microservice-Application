package com.sopuro.account_service;

import com.sopuro.account_service.dtos.PaymentRequestMessageDTO;
import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.dtos.CreateAccountRequestDTO;
import com.sopuro.account_service.dtos.PublicAccountDetailsDTO;
import com.sopuro.account_service.enums.UserStatus;
import com.sopuro.account_service.services.AccountPublicServiceImpl;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.web.PagedModel;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountPublicController {
    private final AccountPublicServiceImpl accountService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping()
    public PrivateAccountDetailsDTO createAccount(
            @RequestHeader("x-user-id") UUID userId,
            @RequestHeader("x-user-status") UserStatus userStatus,
            @Valid @RequestBody CreateAccountRequestDTO request
    ) {
        return accountService.createAccount(userId, userStatus, request.getType(), BigDecimal.ZERO);
    }

    @GetMapping()
    public PagedModel<PrivateAccountDetailsDTO> getUserPrivateAccounts(
            @RequestHeader("x-user-id") UUID userId,
            @RequestHeader("x-user-status") UserStatus userStatus,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(5) @Max(15) int size
    ) {
        log.info("Fetching accounts for user with status: {}", userStatus);
        return accountService.getUserPrivateAccounts(userId, page, size);
    }

    @GetMapping("/{accountNumber}")
    public PublicAccountDetailsDTO getPublicAccountInfo(
            @PathVariable @NotBlank String accountNumber
    ) {
        log.info("Fetching account details for account number: {}", accountNumber);
        return accountService.getPublicAccountInfo(accountNumber);
    };

    @GetMapping("/test-message")
    @Transactional("transactionManager")
    public void testMessage() {
        log.info("Sending test message to Kafka");
        kafkaTemplate.send(
                "payment-requests",
                PaymentRequestMessageDTO.builder()
                        .paymentId(UUID.randomUUID())
                        .senderAccountNumber("CCMMRJIDU0LHOCZ1")
                        .recipientAccountNumber("0XUPPYM4MYNJJ3NHA")
                        .amount(BigDecimal.valueOf(100.00))
                        .build());
        log.info("Test message sent successfully");
    }
}
