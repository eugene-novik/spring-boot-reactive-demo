package io.github.eugenenovik.file.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final Path tempDir = Path.of(System.getProperty("user.dir"), "temp");


  @AfterEach
  void cleanup() throws IOException {
    Files.walk(tempDir)
        .filter(Files::isRegularFile)
        .filter(path -> path.endsWith("test.txt"))
        .forEach(path -> path.toFile().delete());
  }

  @Test
  void testUploadFileSuccess() throws Exception {
    //given
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test.txt",
        MediaType.TEXT_PLAIN_VALUE,
        "Hello World".getBytes()
    );

    MockMultipartFile metadata = new MockMultipartFile(
        "metadata",
        "",
        MediaType.APPLICATION_JSON_VALUE,
        "{\"documentName\":\"Test\"}".getBytes()
    );

    //expect
    mockMvc.perform(multipart("/api/v1/files")
            .file(file)
            .file(metadata))
        .andExpect(status().isOk());

    Assertions.assertTrue(Files.exists(tempDir.resolve("test.txt")));
  }

  @Test
  void testDownloadFileNotFound() throws Exception {
    //given
    UUID fileId = UUID.randomUUID();

    //expect
    mockMvc.perform(get("/api/v1/files/{fileID}", fileId))
        .andExpect(status().isNotFound());
  }

  @Test
  void testDownloadFileSuccess() throws Exception {
    //given
    String fileName = "english-words.pdf";
    UUID fileId = UUID.fromString("d56ada6a-ae30-4229-9030-93a3219db85a");

    //expect
    mockMvc.perform(get("/api/v1/files/{fileID}", fileId))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
        );
  }
}
