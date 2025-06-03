package com.sopuro.payment_service.relays;

import com.sopuro.payment_service.entities.PaymentRequestEntity;
import com.sopuro.payment_service.repositories.PaymentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRequestOutboxPublisher {
    private final PaymentRequestRepository paymentRequestRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka-topics.outbox.payment-requests-topic:payment-requests}")
    private String kafkaTopic;

    @Transactional("kafkaTransactionManager")
    public void publishBatchAndMarkAsSent(List<PaymentRequestEntity> paymentsToProcess) {
        if (paymentsToProcess.isEmpty()) {
            log.debug("No payment requests to publish in this batch.");
            return;
        }

        kafkaTemplate.executeInTransaction(kafkaOperations -> {
            List<UUID> successfullyPublishedIds = paymentsToProcess.stream()
                    .map(payment -> {
                        log.debug("Publishing payment with ID: {}", payment.getPaymentId());
                        kafkaOperations.send(kafkaTopic, payment);
                        return payment.getPaymentId();
                    })
                    .collect(Collectors.toList());

            if (!successfullyPublishedIds.isEmpty()) {
                int updatedCount = paymentRequestRepository.updateIsSentByIdIn(successfullyPublishedIds);
                log.info("Marked {} outbox messages as sent in the database.", updatedCount);
            }
            return true;
        });
        log.info("Successfully published and marked as sent a batch of {} outbox messages.", paymentsToProcess.size());
    }
}
