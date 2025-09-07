package io.github.eugenenovik.file.service;

import io.github.eugenenovik.file.service.exception.FileAlreadyExistsException;
import io.github.eugenenovik.file.service.exception.FileNotFoundException;
import io.github.eugenenovik.file.service.exception.InvalidFileNameException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@AllArgsConstructor
@Slf4j
public class FileMetadataService {

  private final FileMetadataRepository repository;

  public void validateFileName(String fileName) {

    if (!StringUtils.hasText(fileName)) {
      throw new InvalidFileNameException("fileName can't be null");
    }

    repository.findByName(fileName).ifPresent(fileMetadata -> {
      throw new FileAlreadyExistsException(fileMetadata.getId());
    });
  }

  public String getFileNameByID(UUID fileID) {
    FileMetadata fileMetadata = repository.findById(fileID)
        .orElseThrow(() -> {
          log.info("FileMetadata not found for {}", fileID);
          return new FileNotFoundException(fileID);
        });

    return fileMetadata.getName();
  }

  public UUID saveFileMetaData(FileMetadataDTO dto) {
    FileMetadata entity = new FileMetadata();
    entity.setId(UUID.randomUUID());
    entity.setName(dto.name());

    var saved = repository.save(entity);
    return saved.getId();
  }
}
