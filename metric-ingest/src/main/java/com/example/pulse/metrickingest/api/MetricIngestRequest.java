package com.example.pulse.metrickingest.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record MetricIngestRequest(
    @NotNull Instant ts,
    @NotBlank @Size(max = 64) String service,
    @NotBlank @Size(max = 128) String name,
    @NotNull MetricType type,
    @NotNull Double value,
    Map<String, String> tags
) {}

