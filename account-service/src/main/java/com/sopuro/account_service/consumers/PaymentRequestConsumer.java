package com.sopuro.account_service.consumers;

import com.sopuro.account_service.dtos.PaymentRequestMessageDTO;
import com.sopuro.account_service.entities.ProcessedPaymentEntity;
import com.sopuro.account_service.enums.PaymentStatus;
import com.sopuro.account_service.exceptions.account.AccountNotFound;
import com.sopuro.account_service.exceptions.account.InsufficientBalance;
import com.sopuro.account_service.repositories.ProcessedPaymentRepository;
import com.sopuro.account_service.services.AccountAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "payment-requests", groupId = "payment-request-consumer", concurrency = "3")
@RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        exclude = {InsufficientBalance.class, AccountNotFound.class})
public class PaymentRequestConsumer {
    private final ProcessedPaymentRepository processedPaymentRepository;
    private final AccountAdminService accountAdminService;

    @KafkaHandler
    @Async
    public void listen(PaymentRequestMessageDTO message) {
        if (isDuplicatePayment(message.getPaymentId())) {
            log.info("Payment request with ID {} has already been processed", message.getPaymentId());
            return;
        }

        try {
            // Process payment in separate transaction
            accountAdminService.processPayment(message);
        } catch (InsufficientBalance e) {
            saveFailedPayment(message.getPaymentId(), "Sender's account has insufficient funds");
            log.warn("Payment {} failed: insufficient funds for account {}",
                    message.getPaymentId(), message.getSenderAccountNumber());

        } catch (AccountNotFound e) {
            saveFailedPayment(message.getPaymentId(), "Receiver's account does not exist");
            log.warn("Payment {} failed: receiver account {} not found",
                    message.getPaymentId(), message.getRecipientAccountNumber());
        }
    }

    protected boolean isDuplicatePayment(UUID paymentId) {
        return processedPaymentRepository.findById(paymentId).isPresent();
    }

    protected void saveFailedPayment(UUID paymentId, String failureReason) {
        ProcessedPaymentEntity failedPayment = ProcessedPaymentEntity.builder()
                .paymentId(paymentId)
                .status(PaymentStatus.FAILED)
                .failureReason(failureReason)
                .build();
        processedPaymentRepository.save(failedPayment);
    }

    @DltHandler
    public void handleDlt(PaymentRequestMessageDTO message) {
        log.error("Handling DLT for message: {}", message);
        saveFailedPayment(message.getPaymentId(), "Maximum retry attempts exceeded");
    }
}