package com.example.pulse.metrickingest.service;

import com.example.pulse.common.id.SnowflakeIdGenerator;
import com.example.pulse.common.tags.TagUtil;
import com.example.pulse.metrickingest.api.MetricIngestRequest;
import com.example.pulse.metrickingest.api.MetricIngestResult;
import com.example.pulse.metrickingest.api.MetricType;
import com.example.pulse.storage.dao.MetricWriteDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class MetricIngestService {
    private final MetricWriteDao dao;
    private final SnowflakeIdGenerator ids;

    public MetricIngestService(
        MetricWriteDao dao,
        @Value("${pulse.worker-id:1}") int workerId
    ) {
        this.dao = dao;
        this.ids = new SnowflakeIdGenerator(workerId);
    }

    /**
     * v1 정책: COUNTER는 delta(증분)로 입력받는다.
     * - rollup_1m.sum_v가 분당 카운트 역할을 하므로 Grafana에 유리
     */
    @Transactional
    public MetricIngestResult ingest(MetricIngestRequest req) {
        if (req.type() == MetricType.COUNTER && req.value() < 0) {
            throw new IllegalArgumentException("COUNTER value must be delta >= 0 in v1");
        }

        long pointId = ids.nextId();
        String tagsText = TagUtil.canonicalize(req.tags());
        String tagsKey = TagUtil.tagsKey(req.tags());

        Instant ts = req.ts();
        Instant bucket = ts.truncatedTo(ChronoUnit.MINUTES);

        dao.insertPoint(pointId, ts, req.service(), req.name(), req.type().name(), req.value(), tagsText);
        boolean rollupUpdated = dao.upsertRollup1m(bucket, req.service(), req.name(), tagsKey, req.value());

        return new MetricIngestResult(pointId, rollupUpdated);
    }
}

