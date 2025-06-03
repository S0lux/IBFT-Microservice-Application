package com.sopuro.account_service.relays;

import com.sopuro.account_service.entities.ProcessedPaymentEntity;
import com.sopuro.account_service.repositories.ProcessedPaymentRepository;
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
public class ProcessedPaymentPublisher {
    private final ProcessedPaymentRepository processedPaymentRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.outbox.kafka-topic:processed-payments}")
    private String kafkaTopic;

    @Transactional("transactionManager")
    public void publishBatchAndMarkAsSent(List<ProcessedPaymentEntity> paymentsToProcess) {
        if (paymentsToProcess.isEmpty()) {
            log.debug("No payments to publish in this batch.");
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
                int updatedCount = processedPaymentRepository.markAsSent(successfullyPublishedIds);
                log.info("Marked {} outbox messages as sent in the database.", updatedCount);
            }
            return true;
        });
        log.info("Successfully published and marked as sent a batch of {} outbox messages.", paymentsToProcess.size());
    }
}
