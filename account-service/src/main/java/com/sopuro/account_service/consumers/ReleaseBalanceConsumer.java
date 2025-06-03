package com.sopuro.account_service.consumers;

import com.sopuro.account_service.dtos.ReleaseWithheldBalanceMessageDTO;
import com.sopuro.account_service.exceptions.account.AccountNotFound;
import com.sopuro.account_service.exceptions.account.InsufficientBalance;
import com.sopuro.account_service.services.AccountAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@KafkaListener(topics = "release-requests", groupId = "release-request-consumer")
@RetryableTopic(
        attempts = "4",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        exclude = {AccountNotFound.class, InsufficientBalance.class})
public class ReleaseBalanceConsumer {
    private final AccountAdminService accountAdminService;

    @KafkaHandler
    public void processReleaseRequest(ReleaseWithheldBalanceMessageDTO message) throws Exception {
        try {
            accountAdminService.releaseWithheldBalance(message.getAccountNumber(), message.getAmount());
            log.info("Successfully processed release request: {}", message);
        } catch (AccountNotFound e) {
            log.warn("Release request failed: account {} not found", message.getAccountNumber());
        } catch (InsufficientBalance e) {
            log.warn("Release request failed: insufficient withheld balance for account {}", message.getAccountNumber());
        } catch (Exception e) {
            log.error("Unexpected error processing release request: {}", message, e);
            throw e; // Rethrow to trigger retry
        }
    }

    @DltHandler
    public void handleDlt(ReleaseWithheldBalanceMessageDTO message) {
        log.error("Handling DLT for release request: {}", message);
    }
}
