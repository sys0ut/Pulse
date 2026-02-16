package com.example.pulse.storage.model;

import java.time.Instant;

public record LogWriteRequest(
    long logId,
    Instant ts,
    String service,
    String level,
    String message,
    boolean truncated,
    int fullLen,
    String traceId,
    String spanId,
    String tagsText,
    String host
) {}

