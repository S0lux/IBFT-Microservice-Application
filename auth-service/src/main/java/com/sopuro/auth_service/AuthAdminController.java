package com.sopuro.auth_service;

import com.sopuro.auth_service.dtos.UserDetailsDTO;
import com.sopuro.auth_service.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/v1/auth-admin")
public class AuthAdminController {

    private final AuthService authService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailsDTO> getUserDetails(@PathVariable String userId) {
        UserDetailsDTO userDetails = authService.getUserById(userId);
        if (userDetails == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(userDetails);
    }
}

