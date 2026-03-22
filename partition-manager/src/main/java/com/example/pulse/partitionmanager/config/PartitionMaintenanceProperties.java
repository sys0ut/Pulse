package com.example.pulse.partitionmanager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Partition DDL is environment-specific; templates default empty so the job is a no-op until
 * configured. Use {@code {day}} in a template as a placeholder for {@code yyyyMMdd} (UTC).
 */
@ConfigurationProperties(prefix = "pulse.partition")
public record PartitionMaintenanceProperties(
    boolean enabled,
    String createSqlTemplate,
    String dropSqlTemplate) {

  public PartitionMaintenanceProperties {
    if (createSqlTemplate == null) {
      createSqlTemplate = "";
    }
    if (dropSqlTemplate == null) {
      dropSqlTemplate = "";
    }
  }
}
