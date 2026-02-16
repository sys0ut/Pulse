package com.example.pulse.metrickingest.api;

import com.example.pulse.metrickingest.service.MetricIngestService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/metrics")
public class MetricIngestController {
    private final MetricIngestService service;

    public MetricIngestController(MetricIngestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<MetricIngestResponse> ingest(@Valid @RequestBody MetricIngestRequest req) {
        MetricIngestResult r = service.ingest(req);
        return ResponseEntity.status(201).body(new MetricIngestResponse(r.pointId(), r.rollupUpdated()));
    }
}

