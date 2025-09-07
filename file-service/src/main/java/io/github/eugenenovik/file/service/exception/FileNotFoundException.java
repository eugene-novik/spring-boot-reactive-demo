package io.github.eugenenovik.file.service.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class FileNotFoundException extends RuntimeException {

  public FileNotFoundException(UUID id) {
    super("File metadata not found for: " + id);
  }

  public FileNotFoundException(String fileName) {
    super("File: " + fileName + " not found on disk.");
  }
}
