package io.github.eugenenovik.file.service;

import io.github.eugenenovik.file.service.exception.FileNotFoundException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
@Slf4j
public class FileStreamingService {

  public void writeFileByParts(MultipartFile file, Path filePath) throws IOException {
    try (InputStream in = file.getInputStream(); OutputStream out = Files.newOutputStream(
        filePath)) {

      byte[] buffer = new byte[4096];
      int bytesRead;
      long total = 0;
      while ((bytesRead = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesRead);
        out.flush();
        total += bytesRead;
        log.info("Written {} bytes", total);
      }
    }
  }

  public StreamingResponseBody retrieveFileByParts(PathResource resource) {
    return outputStream -> {
      try (InputStream inputStream = resource.getInputStream();
          BufferedInputStream bis = new BufferedInputStream(inputStream)) {

        byte[] buffer = new byte[4096];
        int bytesRead;
        long total = 0;

        while ((bytesRead = bis.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
          outputStream.flush();
          total += bytesRead;
          log.info("Sent {} bytes from file-service", total);
        }

      } catch (IOException ex) {
        log.error("Error while streaming file {}", resource.getFilename(), ex);
        throw new RuntimeException("Error streaming file: " + resource.getFilename(), ex);
      }
    };
  }

}
