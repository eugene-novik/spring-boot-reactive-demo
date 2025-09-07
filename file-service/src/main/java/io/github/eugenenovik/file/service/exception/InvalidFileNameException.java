package io.github.eugenenovik.file.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid fileName")
public class InvalidFileNameException extends RuntimeException {

  public InvalidFileNameException() {
  }

  public InvalidFileNameException(String message) {
    super(message);
  }
}