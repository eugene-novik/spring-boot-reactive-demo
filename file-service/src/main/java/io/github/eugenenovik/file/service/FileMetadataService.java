package io.github.eugenenovik.file.service;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class FileMetadataService {

  private final FileMetadataRepository repository;

  public String getFilePathByID(UUID fileID) {
    FileMetadata fileMetadata = repository.findById(fileID)
        .orElseThrow(() -> {
          log.info("FileMetadata not found for {}", fileID);
          return new FileNotFoundException();
        });

    return fileMetadata.getPath();
  }
}
