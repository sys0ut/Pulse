package com.example.pulse.queryapi.service;

import com.example.pulse.common.api.MetricTimeSeriesResponse;
import com.example.pulse.queryapi.persistence.repo.MetricRollup1mRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MetricQueryService {
    private final MetricRollup1mRepository repo;

    public MetricQueryService(MetricRollup1mRepository repo) {
        this.repo = repo;
    }

    public MetricTimeSeriesResponse timeseries(String serviceName, String name, Instant from, Instant to, String tagsKey) {
        String key = (tagsKey == null || tagsKey.isBlank()) ? "-" : tagsKey;
        var rows = repo.range(serviceName, name, key, from, to);

        var points = rows.stream()
            .map(r -> {
                long cnt = r.getCnt();
                double sum = r.getSumV();
                double avg = cnt == 0 ? 0.0 : sum / cnt;
                return new MetricTimeSeriesResponse.Point(
                    r.getId().getBucketTs(),
                    avg,
                    r.getMinV(),
                    r.getMaxV(),
                    cnt
                );
            })
            .toList();

        return new MetricTimeSeriesResponse(serviceName, name, key, 60, points);
    }
}

