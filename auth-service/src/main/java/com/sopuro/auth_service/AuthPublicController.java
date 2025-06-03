package com.sopuro.auth_service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sopuro.auth_service.dtos.AuthResponseDTO;
import com.sopuro.auth_service.dtos.LoginRequestDTO;
import com.sopuro.auth_service.dtos.RefreshTokenRequestDTO;
import com.sopuro.auth_service.dtos.UserDetailsDTO;
import com.sopuro.auth_service.dtos.UserRegistrationRequestDTO;
import com.sopuro.auth_service.services.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/auth")
public class AuthPublicController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserDetailsDTO> register(@Valid @RequestBody UserRegistrationRequestDTO registrationRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.createUser(registrationRequest));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<AuthResponseDTO> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequestDTO request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserDetailsDTO> getCurrentUser() {
        return ResponseEntity.ok(authService.getCurrentUser());
    }
}
