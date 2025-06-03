package com.sopuro.auth_service.exceptions.registration;

import com.sopuro.auth_service.exceptions.ApplicationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class EmailTakenException extends ApplicationException {
    private final String email;

    public EmailTakenException(String email) {
        super("EMAIL_IN_USE", String.format("The email %s is already in use", email), HttpStatus.CONFLICT);
        this.email = email;
    }
}
