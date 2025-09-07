package io.github.eugenenovik.file.service;

public class FileAlreadyExistsException extends RuntimeException {
  public FileAlreadyExistsException(String message) {
    super(message);
  }
}
