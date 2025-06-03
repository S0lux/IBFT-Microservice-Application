package com.sopuro.account_service.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatusCode;

@Getter
public class ApplicationException extends RuntimeException {
  private final HttpStatusCode statusCode;
  private final String applicationError;
  private final String message;

  public ApplicationException(String applicationError, String message, HttpStatusCode statusCode) {
    super(message);
    this.applicationError = applicationError;
    this.message = message;
    this.statusCode = statusCode;
  }
}
