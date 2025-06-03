package com.sopuro.payment_service.feigns;

import com.sopuro.payment_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.payment_service.dtos.PublicAccountDetailsDTO;
import com.sopuro.payment_service.dtos.WithholdBalanceResponseDTO;
import com.sopuro.payment_service.dtos.WithholdRequestDTO;
import jakarta.validation.constraints.NotBlank;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service")
public interface AccountServiceClient {
    @GetMapping("v1/accounts/{accountNumber}")
    PublicAccountDetailsDTO getPublicAccountInfo(
            @PathVariable @NotBlank String accountNumber
    );

    @PostMapping("/v1/accounts-admin/{accountNumber}/with-hold")
    WithholdBalanceResponseDTO withHoldAvailableBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody WithholdRequestDTO request);

    @GetMapping("/v1/accounts-admin/{accountNumber}")
    PrivateAccountDetailsDTO getAccountDetails(@PathVariable("accountNumber") String accountNumber);
}
