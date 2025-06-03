package com.sopuro.payment_service.relays;

import com.sopuro.payment_service.entities.PaymentRequestEntity;
import com.sopuro.payment_service.repositories.PaymentRequestRepository;
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
public class PaymentRequestRelay {

    private final PaymentRequestRepository paymentRequestRepository;
    private final PaymentRequestOutboxPublisher outboxMessagePublisher;

    @Value("${app.outbox.page-size:5000}")
    private int pageSize;

    @Scheduled(fixedDelayString = "${app.outbox.publish-delay-ms:5000}")
    public void publishOutboxMessages() {
        log.debug("Attempting to publish outbox messages...");

        try {
            PageRequest pageRequest = PageRequest.of(0, pageSize);
            Page<PaymentRequestEntity> processedPaymentsPage =
                    paymentRequestRepository.findByIsSent(false, pageRequest);

            if (processedPaymentsPage.hasContent()) {
                List<PaymentRequestEntity> paymentsToProcess = processedPaymentsPage.getContent();
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