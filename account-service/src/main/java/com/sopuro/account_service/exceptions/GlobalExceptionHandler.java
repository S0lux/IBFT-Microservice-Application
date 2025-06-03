package com.sopuro.account_service.exceptions;

import com.sopuro.account_service.dtos.ErrorResponseDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ErrorResponseDTO> handleApplicationException(ApplicationException ex) {
        ErrorResponseDTO errorResponse = ErrorResponseDTO.builder()
                .code(ex.getApplicationError())
                .status(ex.getStatusCode().value())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(errorResponse, ex.getStatusCode());
    }
}