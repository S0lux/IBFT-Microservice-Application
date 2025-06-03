package com.sopuro.payment_service.dtos;

import com.sopuro.payment_service.enums.UserStatus;
import com.sopuro.payment_service.enums.UserType;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class PrivateUserDetailsDTO {
    private UUID id;
    private UserStatus status;
    private UserType type;
    private String phoneNumber;
    private String email;
    private String fullName;
    private Instant createdAt;
}
