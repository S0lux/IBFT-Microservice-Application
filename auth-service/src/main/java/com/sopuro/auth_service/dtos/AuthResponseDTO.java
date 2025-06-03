package com.sopuro.auth_service.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponseDTO {
    private String accessToken;
    private String refreshToken;
    private UserDetailsDTO user;
}
