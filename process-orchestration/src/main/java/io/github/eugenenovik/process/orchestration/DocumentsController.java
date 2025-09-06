package io.github.eugenenovik.process.orchestration;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/documents")
@Slf4j
public class DocumentsController {

  private final WebClient webClient;

  public DocumentsController(WebClient.Builder webClientBuilder, FileServiceProperties properties) {
    this.webClient = webClientBuilder.baseUrl(properties.getBaseUrl()).build();
  }

  @GetMapping(value = "/{documentID}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
  public Mono<Void> proxyDocument(@PathVariable UUID documentID, ServerHttpResponse response) {

    Flux<DataBuffer> fileStream = webClient.get()
        .uri("api/v1/files/{fileID}", documentID)
        .retrieve()
        .onStatus(HttpStatusCode::isError, clientResponse -> {
          log.error("Can't stream file {}, status code {}", documentID,
              clientResponse.statusCode());
          return clientResponse.createException();
        })
        .bodyToFlux(DataBuffer.class)
        .doOnNext(dataBuffer -> log.info("Got bytes from file service: {} byte",
            dataBuffer.readableByteCount()));

    return response.writeWith(fileStream)
        .onErrorResume(WebClientResponseException.class, ex -> {
          HttpStatusCode statusCode = ex.getStatusCode();
          response.setStatusCode(statusCode);
          return Mono.empty();
        })
        .then();
  }

}
