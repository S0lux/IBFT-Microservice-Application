package com.sopuro.notification_service;

import com.sopuro.notification_service.dtos.NotificationPayloadMessage;
import com.sopuro.notification_service.enums.NotificationType;
import com.sopuro.notification_service.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationOrchestrator {

    private final Map<NotificationType, NotificationSender> senders;

    @Autowired
    public NotificationOrchestrator(List<NotificationSender> senderList) {
        this.senders = senderList.stream()
                .collect(Collectors.toMap(NotificationSender::getNotificationType, Function.identity()));
        log.info("Initialized NotificationOrchestrator with senders: {}", this.senders.keySet());
    }

    public void processNotification(NotificationPayloadMessage payload) {
        log.info("Processing notification for transactionId: {}", payload.getTransactionId());

        List<NotificationType> targetChannels = payload.getTargetChannels();

        if (targetChannels == null || targetChannels.isEmpty()) {
            log.warn("No target channels specified for transactionId: {}.", payload.getTransactionId());
            return;
        }

        log.debug("Target channels for transactionId {}: {}", payload.getTransactionId(), targetChannels);

        for (NotificationType type : targetChannels) {
            NotificationSender sender = senders.get(type);
            if (sender != null) {
                try {
                    log.info("Dispatching notification for transactionId {} via {}", payload.getTransactionId(), type);
                    sender.sendNotification(payload);
                } catch (Exception e) {
                    log.error("Failed to send notification for transactionId {} via {}: {}",
                            payload.getTransactionId(), type, e.getMessage(), e);
                }
            } else {
                log.warn("No sender configured for notification type: {} for transactionId: {}", type, payload.getTransactionId());
            }
        }
        log.info("Finished processing all targeted notifications for transactionId: {}", payload.getTransactionId());
    }
}
