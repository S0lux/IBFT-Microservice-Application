package com.sopuro.auth_service.exceptions.registration;

import com.sopuro.auth_service.exceptions.ApplicationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NumberTakenException extends ApplicationException {
    private final String phoneNumber;

    public NumberTakenException(String phoneNumber) {
        super("PHONE_NUMBER_IN_USE", String.format("Phone number %s is already in use", phoneNumber), HttpStatus.CONFLICT);
        this.phoneNumber = phoneNumber;
    }
}
