package com.sopuro.payment_service.consumers;

import com.sopuro.payment_service.dtos.*;
import com.sopuro.payment_service.entities.PaymentEntity;
import com.sopuro.payment_service.enums.NotificationType;
import com.sopuro.payment_service.enums.PaymentStatus;
import com.sopuro.payment_service.feigns.AccountServiceClient;
import com.sopuro.payment_service.feigns.AuthServiceClient;
import com.sopuro.payment_service.repositories.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedPaymentConsumer {
    private final PaymentRepository paymentRepository;
    private final AccountServiceClient accountServiceClient;
    private final AuthServiceClient authServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @KafkaListener(topics = "processed-payments", groupId = "transactions-updating-group")
    @RetryableTopic(
            dltTopicSuffix = "-transactions-updating-group-dlt",
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalStateException.class})
    public void updateTransactionStatus(ProcessedPaymentMessageDTO message) {
        try {
            Optional<PaymentEntity> paymentOptional = paymentRepository.findById(message.getPaymentId());
            if (paymentOptional.isEmpty()) {
                log.warn("Payment with ID {} does not exist in the database, skipping update", message.getPaymentId());
                return;
            }
            PaymentEntity payment = paymentOptional.get();

            PrivateAccountDetailsDTO senderAccountDetails = accountServiceClient.getAccountDetails(payment.getSenderAccountNumber());
            PrivateAccountDetailsDTO receiverAccountDetails = accountServiceClient.getAccountDetails(payment.getRecipientAccountNumber());

            if (message.getStatus() == PaymentStatus.SUCCEED) {
                payment.setStatus(PaymentStatus.SUCCEED);
                payment.setSenderFinalBalance(senderAccountDetails.getAvailableBalance());
                payment.setRecipientFinalBalance(receiverAccountDetails.getAvailableBalance());
                payment.setFailureReason(null);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(message.getFailureReason());
                log.warn("Payment {} failed: {}", message.getPaymentId(), message.getFailureReason());
            }

            paymentRepository.save(payment);
        } catch (Exception e) {
            log.error("Error processing payment {}", message.getPaymentId(), e);
            throw e; // Rethrow to trigger retry mechanism
        }
    }

    @KafkaListener(topics = "processed-payments", groupId = "notifications-publishing-group")
    @RetryableTopic(
            dltTopicSuffix = "-notifications-publishing-group-dlt",
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            exclude = {IllegalStateException.class})
    public void publishNotification(ProcessedPaymentMessageDTO message) {
        try {
            Optional<PaymentEntity> payment = paymentRepository.findById(message.getPaymentId());
            if (payment.isEmpty()) {
                log.warn("Payment with ID {} does not exist in the database, skipping notification", message.getPaymentId());
                return;
            }

            PrivateUserDetailsDTO senderUserDetails = authServiceClient.getUserDetails(payment.get().getSenderId().toString()).getBody();
            PrivateUserDetailsDTO receiverUserDetails = authServiceClient.getUserDetails(payment.get().getRecipientId().toString()).getBody();

            if (senderUserDetails == null || receiverUserDetails == null) {
                log.warn("Sender or receiver user details not found for payment ID {}", message.getPaymentId());
                return;
            }

            NotificationPayloadMessage payload = NotificationPayloadMessage.builder()
                    .transactionId(message.getPaymentId())
                    .senderName(senderUserDetails.getFullName())
                    .senderAccount(payment.get().getSenderAccountNumber())
                    .receiverName(receiverUserDetails.getFullName())
                    .receiverAccount(payment.get().getRecipientAccountNumber())
                    .status(message.getStatus())
                    .timestamp(payment.get().getCreatedAt())
                    .messageDetails(message.getStatus() == PaymentStatus.SUCCEED
                            ? payment.get().getDescription()
                            : "Payment failed: " + message.getFailureReason())
                    .amount(payment.get().getAmount())
                    .recipientEmail(receiverUserDetails.getEmail())
                    .targetChannels(List.of(NotificationType.EMAIL))
                    .build();

            kafkaTemplate.send("notifications", payload);

        } catch (Exception e) {
            log.error("Error publishing notifications {}", message.getPaymentId(), e);
            throw e; // Rethrow to trigger retry mechanism
        }
    }

    @DltHandler
    @KafkaListener(topics = "processed-payments-transactions-updating-group-dlt", groupId = "transactions-updating-group")
    public void handleUpdateTransactionDlt(ProcessedPaymentMessageDTO message,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("Handling DLT for transactions-updating-group - Payment ID: {}, Topic: {}, Exception: {}",
                message.getPaymentId(), topic, exceptionMessage);
    }

    @DltHandler
    @KafkaListener(topics = "processed-payments-notifications-publishing-group-dlt", groupId = "notifications-publishing-group")
    public void handleNotificationDlt(ProcessedPaymentMessageDTO message,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.EXCEPTION_MESSAGE) String exceptionMessage) {
        log.error("Handling DLT for notifications-publishing-group - Payment ID: {}, Topic: {}, Exception: {}",
                message.getPaymentId(), topic, exceptionMessage);
    }
}