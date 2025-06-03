package com.sopuro.payment_service.dtos;

import com.sopuro.payment_service.enums.UserStatus;
import com.sopuro.payment_service.enums.UserType;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserDetailsDTO {
    private UUID id;
    private UserStatus status;
    private UserType type;
    private String fullName;
}
