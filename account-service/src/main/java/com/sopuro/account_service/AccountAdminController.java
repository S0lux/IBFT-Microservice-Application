package com.sopuro.account_service;

import com.sopuro.account_service.dtos.PrivateAccountDetailsDTO;
import com.sopuro.account_service.dtos.WithholdBalanceResponseDTO;
import com.sopuro.account_service.dtos.WithholdRequestDTO;
import com.sopuro.account_service.services.AccountAdminService;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/accounts-admin")
@RequiredArgsConstructor
public class AccountAdminController {
    private final AccountAdminService accountAdminService;

    @GetMapping("{accountNumber}")
    public PrivateAccountDetailsDTO getAccountDetails(@PathVariable("accountNumber") String accountNumber) {
        return accountAdminService.getAccountDetails(accountNumber);
    }

    @PostMapping("{accountNumber}/with-hold")
    public WithholdBalanceResponseDTO withHoldAvailableBalance(
            @PathVariable("accountNumber") String accountNumber,
            @RequestBody WithholdRequestDTO request) {
        return accountAdminService.withholdAvailableBalance(accountNumber, request.getAmount());
    }
}
