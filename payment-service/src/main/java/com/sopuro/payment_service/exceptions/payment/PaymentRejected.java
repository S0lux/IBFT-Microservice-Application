package com.sopuro.payment_service.exceptions.payment;

import com.sopuro.payment_service.exceptions.ApplicationException;
import org.springframework.http.HttpStatus;

public class PaymentRejected extends ApplicationException {
    public PaymentRejected(String rejectReason) {
        super("PAYMENT_REQUEST_REJECTED",
                rejectReason,
                HttpStatus.FORBIDDEN);
    }
}
