package io.github.eugenenovik.file.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "File not found")
public class FileNotFoundException extends RuntimeException {

  public FileNotFoundException() {
  }

  public FileNotFoundException(String message) {
    super(message);
  }
}
