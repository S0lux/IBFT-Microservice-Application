package com.sopuro.account_service.exceptions.account;

import com.sopuro.account_service.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class AccountNotFound extends ApplicationException {
    public AccountNotFound(String accountNumber) {
        super("ACCOUNT_NOT_FOUND",
                String.format("The account number %s does not exist", accountNumber),
                HttpStatus.NOT_FOUND);
    }
}
