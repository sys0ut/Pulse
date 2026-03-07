package com.example.pulse.dbexporter;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cubrid")
public record CubridExporterProperties(
    List<String> dbList,
    Duration scrapeInterval,
    Duration commandTimeout,
    String bin,
    boolean brokerFs1Enabled,
    Duration brokerFs1ScrapeInterval,
    Duration brokerFs1CaptureDuration,
    int brokerFs1SampleSeconds) {
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

    if (brokerFs1ScrapeInterval == null) {
      brokerFs1ScrapeInterval = Duration.ofSeconds(1);
    }
    // Spring's scheduleWithFixedDelay requires a strictly positive delay.
    if (brokerFs1ScrapeInterval.isZero() || brokerFs1ScrapeInterval.isNegative()) {
      brokerFs1ScrapeInterval = Duration.ofSeconds(1);
    }
    if (brokerFs1CaptureDuration == null) {
      // Capture long enough to include at least 2 snapshots: first (cumulative) + next (1s rate).
      brokerFs1CaptureDuration = Duration.ofSeconds(2);
    }
    if (brokerFs1SampleSeconds <= 0) {
      brokerFs1SampleSeconds = 1;
    }
  }
}

