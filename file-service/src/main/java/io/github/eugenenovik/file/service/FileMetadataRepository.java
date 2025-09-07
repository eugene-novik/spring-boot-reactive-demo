package io.github.eugenenovik.file.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetadataRepository extends CrudRepository<FileMetadata, UUID> {

  Optional<FileMetadata> findByName(String name);

}
