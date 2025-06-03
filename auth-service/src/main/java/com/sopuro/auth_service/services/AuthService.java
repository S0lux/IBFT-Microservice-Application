package com.sopuro.auth_service.services;

import com.sopuro.auth_service.dtos.AuthResponseDTO;
import com.sopuro.auth_service.dtos.LoginRequestDTO;
import com.sopuro.auth_service.dtos.RefreshTokenRequestDTO;
import com.sopuro.auth_service.dtos.UserDetailsDTO;
import com.sopuro.auth_service.dtos.UserRegistrationRequestDTO;

public interface AuthService {
    UserDetailsDTO createUser(UserRegistrationRequestDTO registrationRequest);
    AuthResponseDTO login(LoginRequestDTO loginRequest);
    AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest);
    void logout(String refreshToken);
    UserDetailsDTO getCurrentUser();
    UserDetailsDTO getUserById(String userId);
}
