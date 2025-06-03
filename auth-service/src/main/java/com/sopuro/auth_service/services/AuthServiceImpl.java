package com.sopuro.auth_service.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sopuro.auth_service.dtos.AuthResponseDTO;
import com.sopuro.auth_service.dtos.LoginRequestDTO;
import com.sopuro.auth_service.dtos.RefreshTokenRequestDTO;
import com.sopuro.auth_service.dtos.UserDetailsDTO;
import com.sopuro.auth_service.dtos.UserRegistrationRequestDTO;
import com.sopuro.auth_service.entities.UserEntity;
import com.sopuro.auth_service.exceptions.ApplicationException;
import com.sopuro.auth_service.exceptions.registration.EmailTakenException;
import com.sopuro.auth_service.exceptions.registration.NumberTakenException;
import com.sopuro.auth_service.repositories.UserRepository;
import com.sopuro.auth_service.security.AppUserDetails;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    public UserDetailsDTO createUser(UserRegistrationRequestDTO registrationRequest) {
        log.info("Creating user with phone number: {}", registrationRequest.getPhoneNumber());

        String hashedPassword = passwordEncoder.encode(registrationRequest.getPassword());
        UserEntity userEntity;

        try {
            userEntity = userRepository.save(
                    UserEntity.builder()
                            .phoneNumber(registrationRequest.getPhoneNumber())
                            .passwordHash(hashedPassword)
                            .email(registrationRequest.getEmail())
                            .fullName(registrationRequest.getFullName())
                            .build()
            );
        } catch (DataIntegrityViolationException ex) {
            String lowerCaseMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

            if (lowerCaseMessage.contains("uc_users_phone_number"))
                throw new NumberTakenException(registrationRequest.getPhoneNumber());

            if (lowerCaseMessage.contains("uc_users_email"))
                throw new EmailTakenException(registrationRequest.getEmail());

            log.error("Unexpected DataIntegrityViolationException: {}", ex.getMessage());
            throw new ApplicationException("UNEXPECTED_ERROR", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return UserDetailsDTO.builder()
                .id(userEntity.getId())
                .phoneNumber(userEntity.getPhoneNumber())
                .status(userEntity.getStatus())
                .email(userEntity.getEmail())
                .fullName(userEntity.getFullName())
                .createdAt(userEntity.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getPhoneNumber(),
                        loginRequest.getPassword()
                )
        );

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        UserEntity user = userDetails.getUserEntity();

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        refreshTokenService.createRefreshToken(user, refreshToken);

        return AuthResponseDTO.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserDetailsDTO.builder()
                        .id(user.getId())
                        .phoneNumber(user.getPhoneNumber())
                        .status(user.getStatus())
                        .type(user.getType())
                        .email(user.getEmail())
                        .fullName(user.getFullName())
                        .createdAt(user.getCreatedAt())
                        .build())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO refreshToken(RefreshTokenRequestDTO refreshTokenRequest) {
        try {
            // Verify and get the refresh token entity
            var refreshTokenEntity = refreshTokenService.verifyExpiration(refreshTokenRequest.getRefreshToken());
            UserEntity user = refreshTokenEntity.getUser();
            AppUserDetails userDetails = AppUserDetails.builder().userEntity(user).build();

            // Verify JWT validity
            if (!jwtService.isTokenValid(refreshTokenRequest.getRefreshToken(), userDetails, true)) {
                throw new JwtException("Invalid refresh token");
            }

            // Generate new tokens
            String accessToken = jwtService.generateAccessToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);
            
            // Revoke old refresh token and create new one
            refreshTokenService.revokeRefreshToken(refreshTokenRequest.getRefreshToken());
            refreshTokenService.createRefreshToken(user, newRefreshToken);

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .refreshToken(newRefreshToken)
                    .user(UserDetailsDTO.builder()
                            .id(user.getId())
                            .phoneNumber(user.getPhoneNumber())
                            .status(user.getStatus())
                            .email(user.getEmail())
                            .fullName(user.getFullName())
                            .createdAt(user.getCreatedAt())
                            .build())
                    .build();

        } catch (RuntimeException e) {
            throw new ApplicationException("INVALID_REFRESH_TOKEN", e.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }

    @Override
    public UserDetailsDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ApplicationException("UNAUTHORIZED", "User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        AppUserDetails userDetails = (AppUserDetails) authentication.getPrincipal();
        UserEntity user = userDetails.getUserEntity();

        return UserDetailsDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .type(user.getType())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public UserDetailsDTO getUserById(String userId) {
        UserEntity user = userRepository.findById(UUID.fromString(userId))
                .orElseThrow(() -> new ApplicationException("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND));

        return UserDetailsDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .status(user.getStatus())
                .type(user.getType())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
