package com.example.pulse.logingest.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record LogIngestRequest(
    @NotNull Instant ts,
    @NotBlank @Size(max = 64) String service,
    @NotBlank @Size(max = 16) String level,
    @NotBlank String message,
    @Size(max = 64) String traceId,
    @Size(max = 64) String spanId,
    @Size(max = 64) String host,
    Map<String, String> tags
) {}

