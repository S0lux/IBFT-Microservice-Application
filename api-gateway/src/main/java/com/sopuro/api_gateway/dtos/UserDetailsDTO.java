package com.sopuro.api_gateway.dtos;

import com.sopuro.api_gateway.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDetailsDTO {
    private UUID id;
    private UserStatus status;
    private String phoneNumber;
    private String email;
    private String fullName;
    private Instant createdAt;
}
