package io.github.eugenenovik.process.orchestration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "file.service.api")
@Getter
@Setter
public class FileServiceProperties {
  private String baseUrl;
}
