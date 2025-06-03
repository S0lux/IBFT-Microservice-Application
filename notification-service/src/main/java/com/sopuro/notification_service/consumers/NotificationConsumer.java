package com.sopuro.notification_service.consumers;

import com.sopuro.notification_service.NotificationOrchestrator;
import com.sopuro.notification_service.dtos.NotificationPayloadMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationOrchestrator notificationOrchestrator;

    @KafkaListener(
            topics = "${app.kafka-topics.notifications:notifications}",
            groupId = "notification-consumer-group"
    )
    public void consumeIBFTNotification(
            @Payload NotificationPayloadMessage payload) {

//        log.info("Received IBFT notification message from Kafka.");
//        log.debug("Payload: {}", payload);
//
//        try {
//            notificationOrchestrator.processNotification(payload);
//            log.info("Successfully processed notification for transactionId: {}", payload.getTransactionId());
//        } catch (Exception e) {
//            // Log critical errors during orchestration. Individual sender errors are handled within the orchestrator.
//            log.error("Critical error processing notification for transactionId: {}. Error: {}",
//                    payload.getTransactionId(), e.getMessage(), e);
//        }
    }
}
