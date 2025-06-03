package com.sopuro.payment_service.feigns;

import com.sopuro.payment_service.dtos.PrivateUserDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @GetMapping("/v1/auth-admin/{userId}")
    ResponseEntity<PrivateUserDetailsDTO> getUserDetails(@PathVariable String userId);
}
