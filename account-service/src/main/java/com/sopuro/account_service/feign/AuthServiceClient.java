package com.sopuro.account_service.feign;

import com.sopuro.account_service.dtos.UserDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {
    @GetMapping("/v1/auth-admin/{userId}")
    ResponseEntity<UserDetailsDTO> getUserDetails(@PathVariable String userId);
}
