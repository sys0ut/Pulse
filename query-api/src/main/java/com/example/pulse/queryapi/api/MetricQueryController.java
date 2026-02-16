package com.example.pulse.queryapi.api;

import com.example.pulse.common.api.MetricTimeSeriesResponse;
import com.example.pulse.queryapi.service.MetricQueryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/v1/query/metrics")
public class MetricQueryController {
    private final MetricQueryService service;

    public MetricQueryController(MetricQueryService service) {
        this.service = service;
    }

    @GetMapping("/timeseries")
    public ResponseEntity<MetricTimeSeriesResponse> timeseries(
        @RequestParam String serviceName,
        @RequestParam String name,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
        @RequestParam(defaultValue = "-") String tagsKey
    ) {
        return ResponseEntity.ok(service.timeseries(serviceName, name, from, to, tagsKey));
    }
}

