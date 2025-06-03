package com.sopuro.account_service.exceptions.account;

import com.sopuro.account_service.enums.UserStatus;
import com.sopuro.account_service.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class UserStatusNotValid extends ApplicationException {
    public UserStatusNotValid(UserStatus currentUserStatus) {
        super("USER_STATUS_NOT_VALID",
                String.format("The current user's status (%s) is not allowed", currentUserStatus),
                HttpStatus.FORBIDDEN);
    }
}
