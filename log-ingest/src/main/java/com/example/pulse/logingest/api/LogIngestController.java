package com.example.pulse.logingest.api;

import com.example.pulse.logingest.service.LogIngestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/logs")
public class LogIngestController {
    private final LogIngestService service;

    public LogIngestController(LogIngestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<LogIngestResponse> ingest(@Valid @RequestBody LogIngestRequest req) {
        LogIngestResult r = service.ingest(req);
        return ResponseEntity.status(201).body(new LogIngestResponse(r.logId(), r.truncated()));
    }
}

