package com.example.pulse.dbexporter;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cubrid")
public record CubridExporterProperties(
    List<String> dbList, Duration scrapeInterval, Duration commandTimeout, String bin) {
  public CubridExporterProperties {
    if (commandTimeout == null) {
      commandTimeout = Duration.ofSeconds(5);
    }
    if (scrapeInterval == null) {
      scrapeInterval = Duration.ofSeconds(14);
    }
    if (bin == null || bin.isBlank()) {
      bin = "cubrid";
    }
  }
}

