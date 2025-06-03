package com.sopuro.payment_service.publishers;

import com.sopuro.payment_service.dtos.ReleaseWithheldBalanceMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReleaseBalancePublisher {
    private final KafkaTemplate<String, ReleaseWithheldBalanceMessageDTO> kafkaTemplate;

    @Value("${app.kafka-topics.outbox.release-requests-topic:release-requests}")
    private String RELEASE_WITHHELD_BALANCE_TOPIC;

    @Transactional("kafkaTransactionManager")
    public void publish(ReleaseWithheldBalanceMessageDTO message) {
        try {
            kafkaTemplate.send(RELEASE_WITHHELD_BALANCE_TOPIC, message);
            log.info("Published message to release withheld balance: {}", message);
        } catch (Exception e) {
            log.error("Failed to publish message to release withheld balance: {}", message, e);
            // This is critical and will require manual intervention
        }
    }
}
