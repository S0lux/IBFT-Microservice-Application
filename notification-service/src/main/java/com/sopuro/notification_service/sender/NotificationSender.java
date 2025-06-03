package com.sopuro.notification_service.sender;

import com.sopuro.notification_service.dtos.NotificationPayloadMessage;
import com.sopuro.notification_service.enums.NotificationType;

/**
 * Interface for all notification sending strategies.
 * Each implementation will handle sending a notification via a specific channel (e.g., email, SMS, FCM).
 */
public interface NotificationSender {

    /**
     * Sends the notification based on the provided payload.
     *
     * @param payload The message payload containing details for the notification.
     * @throws Exception if sending the notification fails.
     */
    void sendNotification(NotificationPayloadMessage payload) throws Exception;

    /**
     * Returns the type of notification this sender handles.
     * This is used by the NotificationOrchestrator to map payloads to the correct sender.
     *
     * @return The NotificationType this sender is responsible for.
     */
    NotificationType getNotificationType();
}
