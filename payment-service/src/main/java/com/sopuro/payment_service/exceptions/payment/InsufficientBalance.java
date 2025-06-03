package com.sopuro.payment_service.exceptions.payment;

import com.sopuro.payment_service.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class InsufficientBalance extends ApplicationException {
    public InsufficientBalance(String accountNumber) {
        super("INSUFFICIENT_BALANCE",
                String.format("The account number %s does not have sufficient balance", accountNumber),
                HttpStatus.BAD_REQUEST);
    }
}
