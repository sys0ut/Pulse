package com.example.pulse.queryapi.api;

import java.time.Instant;
import java.util.List;

public record LogQueryResponse(
    List<LogRow> items,
    Instant nextCursorTs,
    Long nextCursorId
) {
    public record LogRow(
        long logId,
        Instant ts,
        String service,
        String level,
        String message,
        boolean truncated,
        String traceId,
        String tagsText
    ) {}
}

