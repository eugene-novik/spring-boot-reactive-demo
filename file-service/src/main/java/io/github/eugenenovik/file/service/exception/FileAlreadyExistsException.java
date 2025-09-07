package io.github.eugenenovik.file.service.exception;


import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class FileAlreadyExistsException extends RuntimeException {

  public FileAlreadyExistsException(UUID id) {
    super("File already exist, id:" + id);
  }
}
