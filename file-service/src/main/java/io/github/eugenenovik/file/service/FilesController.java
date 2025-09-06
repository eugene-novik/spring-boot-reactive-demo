package io.github.eugenenovik.file.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/files")
@Slf4j
@AllArgsConstructor
public class FilesController {

  private final FileMetadataService fileMetadataService;

  @GetMapping("/{fileID}")
  public ResponseEntity<StreamingResponseBody> getFile(@PathVariable UUID fileID) {
    String filePath = fileMetadataService.getFilePathByID(fileID);

    ClassPathResource resource = new ClassPathResource(filePath);

    String contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
    if (contentType == null) {
      contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }

    long contentLength = 0;
    try {
      contentLength = resource.contentLength();
    } catch (IOException ex) {
      log.error("Can't get content-length", ex);
    }

    StreamingResponseBody stream = outputStream -> {
      try (InputStream inputStream = resource.getInputStream()) {
        byte[] buffer = new byte[4096];
        int bytesRead;
        long total = 0;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
          outputStream.flush();
          total += bytesRead;
          log.info("Sent {} bytes from file-service", total);

          Thread.sleep(50);
        }
      } catch (InterruptedException ex) {
        log.error("Error while sleep", ex);
        throw new RuntimeException("Error while sleep", ex);
      }
    };

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(
            HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
        .contentLength(contentLength)
        .body(stream);
  }

}
