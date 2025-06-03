package com.sopuro.account_service.relays;

import com.sopuro.account_service.entities.ProcessedPaymentEntity;
import com.sopuro.account_service.repositories.ProcessedPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessedPaymentRelay {

    private final ProcessedPaymentRepository processedPaymentRepository;
    private final ProcessedPaymentPublisher outboxMessagePublisher;

    @Value("${app.outbox.page-size:5000}")
    private int pageSize;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    public void publishOutboxMessages() {
        log.debug("Attempting to publish outbox messages...");

        try {
            PageRequest pageRequest = PageRequest.of(0, pageSize);
            Page<ProcessedPaymentEntity> processedPaymentsPage =
                    processedPaymentRepository.findByIsSent(false, pageRequest);

            if (processedPaymentsPage.hasContent()) {
                List<ProcessedPaymentEntity> paymentsToProcess = processedPaymentsPage.getContent();
                log.info("Found {} outbox messages to publish.", paymentsToProcess.size());

                try {
                    outboxMessagePublisher.publishBatchAndMarkAsSent(paymentsToProcess);
                } catch (Exception e) {
                    log.error("Error during transactional batch publishing of outbox messages. " +
                            "Transaction rolled back. Error: {}", e.getMessage(), e);
                }
            } else {
                log.debug("No outbox messages to publish.");
            }
        } catch (Exception e) {
            log.error("Error while fetching outbox messages: {}", e.getMessage(), e);
        }
    }
}