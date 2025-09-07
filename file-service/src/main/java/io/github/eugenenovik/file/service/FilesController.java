package io.github.eugenenovik.file.service;

import io.github.eugenenovik.file.service.exception.FileNotFoundException;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/v1/files")
@Slf4j
@AllArgsConstructor
public class FilesController {

  private final FileMetadataService fileMetadataService;
  private final FileStreamingService fileStreamingService;

  private final ExecutorService executor = Executors.newFixedThreadPool(5);

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public DeferredResult<ResponseEntity<Map<String, String>>> uploadFile(
      @RequestPart("file") MultipartFile file,
      @RequestPart("metadata") String metadataJson) {

    DeferredResult<ResponseEntity<Map<String, String>>> result = new DeferredResult<>();


    String newFileName = file.getOriginalFilename();
    fileMetadataService.validateFileName(newFileName);

    executor.submit(() -> {
      Path filePath = Path.of(System.getProperty("user.dir"), "temp", newFileName);

      try {
        fileStreamingService.writeFileByParts(file, filePath);

        FileMetadataDTO dto = new FileMetadataDTO(newFileName);
        UUID fileID = fileMetadataService.saveFileMetaData(dto);

        result.setResult(ResponseEntity.ok(Map.of("fileId", fileID.toString())));
      } catch (IOException e) {
        log.error("Upload failed", e);
        result.setErrorResult(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", e.getMessage())));
      }
    });

    return result;
  }

  @GetMapping("/{fileID}")
  public ResponseEntity<StreamingResponseBody> getFile(@PathVariable UUID fileID) {
    String fileName = fileMetadataService.getFileNameByID(fileID);

    Path filePath = Path.of(System.getProperty("user.dir"), "temp", fileName);
    PathResource resource = new PathResource(filePath);

    if (!resource.exists()) {
      log.error("File not found: {}", resource.getFilename());
      throw new FileNotFoundException(resource.getFilename());
    }

    return ResponseEntity.ok()
        .contentType(getContentType(resource))
        .headers(buildHeaders(resource))
        .contentLength(getContentLength(resource))
        .body(fileStreamingService.retrieveFileByParts(resource));
  }

  private long getContentLength(PathResource resource) {
    long contentLength = 0;
    try {
      contentLength = resource.contentLength();
    } catch (IOException ex) {
      log.error("Can't get content-length", ex);
    }
    return contentLength;
  }

  private HttpHeaders buildHeaders(PathResource resource) {
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.put(HttpHeaders.CONTENT_DISPOSITION,
        Collections.singletonList("attachment; filename=\"" + resource.getFilename() + "\""));
    return httpHeaders;
  }

  private MediaType getContentType(PathResource resource) {
    String contentType = URLConnection.guessContentTypeFromName(resource.getFilename());
    if (contentType == null) {
      contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
    return MediaType.parseMediaType(contentType);
  }

}
