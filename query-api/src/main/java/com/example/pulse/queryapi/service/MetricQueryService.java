package com.example.pulse.queryapi.service;

import com.example.pulse.common.api.MetricTimeSeriesResponse;
import com.example.pulse.storage.dao.MetricQueryDao;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class MetricQueryService {
    private final MetricQueryDao dao;

    public MetricQueryService(MetricQueryDao dao) {
        this.dao = dao;
    }

    public MetricTimeSeriesResponse timeseries(String serviceName, String name, Instant from, Instant to, String tagsKey) {
        return dao.timeseries(serviceName, name, from, to, tagsKey);
    }
}

