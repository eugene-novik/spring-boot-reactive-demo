package io.github.eugenenovik.process.orchestration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/documents")
@Slf4j
public class DocumentsController {

  private final WebClient webClient;

  public DocumentsController(WebClient.Builder webClientBuilder, FileServiceProperties properties) {
    this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Mono<Void> proxyUpload(@RequestPart("file") FilePart file,
      @RequestPart("metadata") String metadataJson, ServerHttpResponse response) {

    log.info("Proxying upload for file {}", file.filename());

    MultipartBodyBuilder bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", file);
    bodyBuilder.part("metadata", metadataJson);

    return webClient.post()
        .uri("/api/v1/files")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .body(BodyInserters.fromMultipartData(bodyBuilder.build()))
        .exchangeToMono(clientResponse -> {
          HttpStatusCode status = clientResponse.statusCode();

          log.info("Upload proxied, got status {}", status.value());

          return clientResponse.bodyToMono(String.class)
              .defaultIfEmpty("")
              .flatMap(body -> {
                response.setStatusCode(status);
                DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
              });
        })
        .onErrorResume(ex -> {
          log.error("Unexpected error in proxy: {}", ex.getMessage());
          response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
          return Mono.empty();
        });
  }

  @GetMapping(value = "/{documentID}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Mono<Void> proxyDocument(@PathVariable UUID documentID, ServerHttpResponse response) {
    return webClient.get()
        .uri("api/v1/files/{fileID}", documentID)
        .exchangeToMono(clientResponse -> {
          HttpStatusCode statusCode = clientResponse.statusCode();

          if (statusCode.isError()) {
            response.setStatusCode(statusCode);
            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

            return clientResponse.bodyToMono(String.class)
                .defaultIfEmpty("Upstream error")
                .flatMap(errorBody -> {
                  byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
                  DataBuffer buffer = response.bufferFactory().wrap(bytes);
                  return response.writeWith(Mono.just(buffer));
                });
          }

          response.setStatusCode(statusCode);
          response.getHeaders().putAll(clientResponse.headers().asHttpHeaders());

          return response.writeWith(clientResponse.bodyToFlux(DataBuffer.class)
              .doOnNext(dataBuffer -> log.info("Got bytes from file service: {} byte",
                  dataBuffer.readableByteCount())));
        });
  }

}
