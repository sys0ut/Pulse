package com.example.pulse.queryapi.api;

import java.time.Instant;
import java.util.List;

public record MetricTimeSeriesResponse(
    String service,
    String name,
    String tagsKey,
    int stepSec,
    List<Point> points
) {
    public record Point(
        Instant t,
        double avg,
        double min,
        double max,
        long count
    ) {}
}

