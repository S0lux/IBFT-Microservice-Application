package com.sopuro.account_service.entities;

import com.sopuro.account_service.enums.UserStatus;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RedisHash("user")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntityCache implements Serializable {
    @Id
    private UUID id;
    private UserStatus status;
    private String phoneNumber;
    private String email;
    private String fullName;
    private Instant createdAt;

    @TimeToLive(unit = TimeUnit.MINUTES)
    @Builder.Default
    private Long timeToLive = 60L;
}