package com.sopuro.payment_service.services;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.sopuro.payment_service.dtos.PaymentDetailsDTO;
import com.sopuro.payment_service.dtos.PaymentRequestDTO;
import com.sopuro.payment_service.dtos.PublicAccountDetailsDTO;
import com.sopuro.payment_service.dtos.ReleaseWithheldBalanceMessageDTO;
import com.sopuro.payment_service.dtos.WithholdRequestDTO;
import com.sopuro.payment_service.entities.PaymentEntity;
import com.sopuro.payment_service.entities.PaymentRequestEntity;
import com.sopuro.payment_service.enums.AccountStatus;
import com.sopuro.payment_service.enums.UserStatus;
import com.sopuro.payment_service.enums.UserType;
import com.sopuro.payment_service.exceptions.ApplicationException;
import com.sopuro.payment_service.exceptions.payment.AccountNotFound;
import com.sopuro.payment_service.exceptions.payment.InsufficientBalance;
import com.sopuro.payment_service.exceptions.payment.PaymentRejected;
import com.sopuro.payment_service.feigns.AccountServiceClient;
import com.sopuro.payment_service.mappers.PaymentMapper;
import com.sopuro.payment_service.publishers.ReleaseBalancePublisher;
import com.sopuro.payment_service.repositories.PaymentRepository;
import com.sopuro.payment_service.repositories.PaymentRequestRepository;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {
    private final AccountServiceClient accountServiceClient;
    private final RedisTemplate<String, String> redisTemplate;
    private final PaymentRepository paymentRepository;
    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentMapper paymentMapper;
    private final ReleaseBalancePublisher releaseBalancePublisher;

    private static final Map<UserType, BigDecimal> DAILY_LIMITS = Map.of(
            UserType.STANDARD, BigDecimal.valueOf(10_000_000),
            UserType.VIP, BigDecimal.valueOf(50_000_000)
    );

    private static final Map<UserType, BigDecimal> MONTHLY_LIMITS = Map.of(
            UserType.STANDARD, BigDecimal.valueOf(100_000_000),
            UserType.VIP, BigDecimal.valueOf(5_000_000_000L)
    );

    private static final String LIMIT_CHECK_SCRIPT = """
        local daily_key = KEYS[1]
        local monthly_key = KEYS[2]
        local amount = tonumber(ARGV[1])
        local daily_limit = tonumber(ARGV[2])
        local monthly_limit = tonumber(ARGV[3])
        local daily_ttl = tonumber(ARGV[4])
        local monthly_ttl = tonumber(ARGV[5])
        
        -- Get current values, initialize if not exists with TTL
        local daily_current = tonumber(redis.call('GET', daily_key) or '0')
        local monthly_current = tonumber(redis.call('GET', monthly_key) or '0')
        
        -- Check limits
        if (daily_current + amount) > daily_limit then
            return 'DAILY_LIMIT_EXCEEDED'
        end
        if (monthly_current + amount) > monthly_limit then
            return 'MONTHLY_LIMIT_EXCEEDED'
        end
        
        -- Increment counters and set/update TTL
        redis.call('INCRBY', daily_key, amount)
        redis.call('EXPIRE', daily_key, daily_ttl)
        
        redis.call('INCRBY', monthly_key, amount)
        redis.call('EXPIRE', monthly_key, monthly_ttl)
        
        return 'SUCCESS'
        """;

    // The script returns a String, so the result type should be String.class
    private final DefaultRedisScript<String> limitCheckScript = new DefaultRedisScript<>(LIMIT_CHECK_SCRIPT, String.class);

    @Override
    @Async
    public CompletableFuture<PaymentDetailsDTO> makePayment(UUID requesterId, PaymentRequestDTO request) {
        log.debug("Processing payment request from {} to {} for amount {}",
                request.getSenderAccountNumber(), request.getRecipientAccountNumber(), request.getAmount());

        // Input validation
        validatePaymentRequest(request);

        // Step 1: Validate accounts
        PublicAccountDetailsDTO senderAccount = validateAndGetAccount(request.getSenderAccountNumber(), "sender");
        PublicAccountDetailsDTO recipientAccount = validateAndGetAccount(request.getRecipientAccountNumber(), "recipient");

        // Ensure the sender is the owner of the sender account
        if (!senderAccount.getOwner().getId().equals(requesterId)) {
            log.warn("Requester {} is not the owner of sender account {}", requesterId, request.getSenderAccountNumber());
            throw new PaymentRejected("Requester is not the owner of the sender account");
        }

        // Ensure the sender is not trying to pay themselves
        if (senderAccount.getNumber().equals(recipientAccount.getNumber())) {
            throw new PaymentRejected("Cannot transfer to the same account");
        }

        // Step 2: Check payment limits
        checkPaymentLimits(request.getSenderAccountNumber(), request.getAmount(), senderAccount.getOwner().getType());

        // Step 3: Withhold amount from the sender's account
        withholdFunds(request);

        try {
            // Step 4: Create payment entities
            PaymentEntity newPayment = createPaymentEntity(request, senderAccount, recipientAccount);
            PaymentRequestEntity paymentRequest = createPaymentRequestEntity(request, newPayment);

            // Step 5: Save payment and request entities
            paymentRepository.save(newPayment);
            paymentRequestRepository.save(paymentRequest);

            return CompletableFuture.completedFuture(paymentMapper.toPaymentDetailsDTO(newPayment));
        } catch (Exception e) {
            log.error("Error saving payment entities: {}", e.getMessage(), e);

            compensateFailedPayment(request);

            throw new ApplicationException("PAYMENT_PROCESSING_FAILED",
                    "Failed to process payment. Please try again later.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validatePaymentRequest(PaymentRequestDTO request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentRejected("Payment amount must be positive");
        }
        if (request.getSenderAccountNumber() == null || request.getSenderAccountNumber().trim().isEmpty()) {
            throw new PaymentRejected("Sender account number is required");
        }
        if (request.getRecipientAccountNumber() == null || request.getRecipientAccountNumber().trim().isEmpty()) {
            throw new PaymentRejected("Recipient account number is required");
        }
        if (request.getSenderAccountNumber().equals(request.getRecipientAccountNumber())) {
            throw new PaymentRejected("Cannot transfer to the same account");
        }
    }

    private PublicAccountDetailsDTO validateAndGetAccount(String accountNumber, String accountType) {
        try {
            PublicAccountDetailsDTO account = accountServiceClient.getPublicAccountInfo(accountNumber);
            ensureAccountValid(account, accountType);
            return account;
        } catch (FeignException.NotFound e) {
            log.warn("{} account not found: {}", accountType, accountNumber);
            throw new AccountNotFound(accountNumber);
        } catch (PaymentRejected e) {
            throw e; // Re-throw validation errors
        } catch (Exception e) {
            log.error("Error fetching {} account details for {}: {}", accountType, accountNumber, e.getMessage());
            throw new ApplicationException("SERVICE_UNAVAILABLE",
                    "Account service is unavailable. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private void checkPaymentLimits(String senderAccountNumber, BigDecimal amount, UserType userType) {
        try {
            String dailyKey = String.format("daily_limit:%s", senderAccountNumber);
            String monthlyKey = String.format("monthly_limit:%s", senderAccountNumber);

            BigDecimal dailyLimit = DAILY_LIMITS.getOrDefault(userType, BigDecimal.ZERO);
            BigDecimal monthlyLimit = MONTHLY_LIMITS.getOrDefault(userType, BigDecimal.ZERO);

            // Execute the Lua script to atomically check and increment limits
            String result = redisTemplate.execute(
                    limitCheckScript,
                    Arrays.asList(dailyKey, monthlyKey),
                    String.valueOf(amount.longValue()),
                    String.valueOf(dailyLimit.longValue()),
                    String.valueOf(monthlyLimit.longValue()),
                    String.valueOf(Duration.ofDays(1).getSeconds()),
                    String.valueOf(Duration.ofDays(30).getSeconds())
            );

            if (result == null || !result.equals("SUCCESS")) {
                String errorMessage = switch (result != null ? result : "LIMIT_CHECK_FAILED") {
                    case "DAILY_LIMIT_EXCEEDED" -> String.format("Daily limit exceeded for account: %s", senderAccountNumber);
                    case "MONTHLY_LIMIT_EXCEEDED" -> String.format("Monthly limit exceeded for account: %s", senderAccountNumber);
                    default -> "Payment limit validation failed";
                };
                throw new PaymentRejected(errorMessage);
            }

            log.debug("Payment limits validated successfully for account: {}", senderAccountNumber);
        } catch (PaymentRejected e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating payment limits for account {}: {}", senderAccountNumber, e.getMessage());
            throw new ApplicationException("SERVICE_UNAVAILABLE",
                    "Payment limit service is unavailable. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BigDecimal withholdFunds(PaymentRequestDTO request) {
        try {
            return accountServiceClient.withHoldAvailableBalance(
                            request.getSenderAccountNumber(),
                            WithholdRequestDTO.builder()
                                    .amount(request.getAmount().add(calculateFee(request.getAmount())))
                                    .build())
                    .getAvailableBalance();
        } catch (FeignException.BadRequest e) {
            log.debug("Insufficient balance for account: {}", request.getSenderAccountNumber());
            throw new InsufficientBalance(request.getSenderAccountNumber());
        } catch (Exception e) {
            log.error("Error withholding balance for account {}: {}", request.getSenderAccountNumber(), e.getMessage());
            throw new ApplicationException("SERVICE_UNAVAILABLE",
                    "Account service is unavailable. Please try again later.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private PaymentEntity createPaymentEntity(
            PaymentRequestDTO request,
            PublicAccountDetailsDTO senderAccount,
            PublicAccountDetailsDTO recipientAccount) {

        BigDecimal fee = calculateFee(request.getAmount());

        return PaymentEntity.builder()
                .id(UUID.randomUUID())
                .senderId(senderAccount.getOwner().getId())
                .senderAccountNumber(request.getSenderAccountNumber())
                .recipientId(recipientAccount.getOwner().getId())
                .recipientAccountNumber(request.getRecipientAccountNumber())
                .amount(request.getAmount())
                .fee(fee)
                .description(request.getDescription())
                .createdAt(Instant.now())
                .build();
    }

    private PaymentRequestEntity createPaymentRequestEntity(PaymentRequestDTO request, PaymentEntity payment) {
        return PaymentRequestEntity.builder()
                .paymentId(payment.getId())
                .senderAccountNumber(request.getSenderAccountNumber())
                .recipientAccountNumber(request.getRecipientAccountNumber())
                .amount(request.getAmount())
                .fee(payment.getFee())
                .isSent(false)
                .build();
    }

    private BigDecimal calculateFee(BigDecimal amount) {
        // 1% fee with minimum and maximum bounds
        BigDecimal fee = amount.multiply(BigDecimal.valueOf(0.01));
        BigDecimal minFee = BigDecimal.valueOf(1000);
        BigDecimal maxFee = BigDecimal.valueOf(100_000);

        if (fee.compareTo(minFee) < 0) return minFee;
        if (fee.compareTo(maxFee) > 0) return maxFee;
        return fee;
    }

    public void compensateFailedPayment(PaymentRequestDTO request) {
        try {
            // Revert the limit increments
            String dailyKey = String.format("daily_limit:%s", request.getSenderAccountNumber());
            String monthlyKey = String.format("monthly_limit:%s", request.getSenderAccountNumber());

            redisTemplate.opsForValue().decrement(dailyKey, request.getAmount().longValue());
            redisTemplate.opsForValue().decrement(monthlyKey, request.getAmount().longValue());

            log.info("Compensated limit increments for failed payment: {}", request.getSenderAccountNumber());
        } catch (Exception e) {
            log.error("Failed to compensate limit increments for account {}: {}",
                    request.getSenderAccountNumber(), e.getMessage());
            // This is a critical issue that needs manual intervention or a retry mechanism
        }

        try {
            // Release withheld balance
            ReleaseWithheldBalanceMessageDTO releaseMessage = ReleaseWithheldBalanceMessageDTO.builder()
                    .accountNumber(request.getSenderAccountNumber())
                    .amount(request.getAmount().add(calculateFee(request.getAmount())))
                    .build();
            releaseBalancePublisher.publish(releaseMessage);
        } catch (Exception e) {
            log.error("Failed to send release request for account {}: {}",
                    request.getSenderAccountNumber(), e.getMessage());
            // This is a critical issue that needs manual intervention or a retry mechanism
        }
    }

    private void ensureAccountValid(PublicAccountDetailsDTO account, String accountType) {
        if (account.getOwner().getStatus() == UserStatus.SUSPENDED) {
            throw new PaymentRejected(String.format("%s account owner is suspended: %s",
                    accountType, account.getOwner().getFullName()));
        }

        if (account.getStatus() == AccountStatus.PAYMENT_LOCKED) {
            throw new PaymentRejected(String.format("%s account is payment locked: %s",
                    accountType, account.getNumber()));
        }
    }

    @Override
    public Page<PaymentDetailsDTO> getPaymentHistory(UUID userId, Pageable pageable) {
        log.info("Fetching payment history for user {}", userId);
        Page<PaymentEntity> paymentPage = paymentRepository.findBySenderIdOrRecipientId(userId, userId, pageable);
        return paymentPage.map(paymentMapper::toPaymentDetailsDTO);
    }
}