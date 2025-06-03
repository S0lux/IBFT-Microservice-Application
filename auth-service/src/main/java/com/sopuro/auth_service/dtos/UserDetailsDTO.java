package com.sopuro.auth_service.dtos;

import com.sopuro.auth_service.enums.UserStatus;
import com.sopuro.auth_service.enums.UserType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDetailsDTO {
    private UUID id;
    private UserStatus status;
    private UserType type;
    private String phoneNumber;
    private String email;
    private String fullName;
    private Instant createdAt;
}
