package com.example.pulse.queryapi.api;

import com.example.pulse.common.api.LogQueryResponse;
import com.example.pulse.queryapi.service.LogQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1/query/logs")
public class LogQueryController {
    private final LogQueryService service;

    public LogQueryController(LogQueryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<LogQueryResponse> query(
        @RequestParam String serviceName,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(required = false) String level,
        @RequestParam(required = false) String traceId,
        @RequestParam(defaultValue = "200") int limit,
        @RequestParam(required = false) Instant cursorTs,
        @RequestParam(required = false) Long cursorId
    ) {
        return ResponseEntity.ok(service.query(serviceName, from, to, level, traceId, limit, cursorTs, cursorId));
    }
}

