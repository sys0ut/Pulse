package com.example.pulse.logingest.service;

import com.example.pulse.common.id.SnowflakeIdGenerator;
import com.example.pulse.common.tags.TagUtil;
import com.example.pulse.logingest.api.LogIngestRequest;
import com.example.pulse.logingest.api.LogIngestResult;
import com.example.pulse.storage.dao.LogWriteDao;
import com.example.pulse.storage.model.LogWriteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class LogIngestService {
    private static final int MESSAGE_MAX_BYTES = 8 * 1024;

    private final LogWriteDao dao;
    private final ObjectMapper om;
    private final SnowflakeIdGenerator ids;
    private final boolean storePayload;

    public LogIngestService(
        LogWriteDao dao,
        ObjectMapper om,
        @Value("${pulse.worker-id:0}") int workerId,
        @Value("${pulse.logs.store-payload:true}") boolean storePayload
    ) {
        this.dao = dao;
        this.om = om;
        this.ids = new SnowflakeIdGenerator(workerId);
        this.storePayload = storePayload;
    }

    @Transactional
    public LogIngestResult ingest(LogIngestRequest req) {
        long id = ids.nextId();

        String message = req.message();
        byte[] bytes = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        boolean truncated = false;
        int fullLen = bytes.length;
        String payload = null;

        if (bytes.length > MESSAGE_MAX_BYTES) {
            truncated = true;
            payload = message;
            message = new String(bytes, 0, MESSAGE_MAX_BYTES, java.nio.charset.StandardCharsets.UTF_8);
        }

        String tagsText;
        try {
            // v1 저장 포맷은 자유지만, 일단 canonical + 원본 JSON 둘 다 쉽게 대응되게 canonical만 저장
            tagsText = TagUtil.canonicalize(req.tags());
            if (tagsText.isEmpty() && req.tags() != null && !req.tags().isEmpty()) {
                tagsText = om.writeValueAsString(req.tags());
            }
        } catch (Exception e) {
            tagsText = "";
        }

        Instant ts = req.ts();
        dao.insertLog(new LogWriteRequest(
            id, ts, req.service(), req.level(), message, truncated, fullLen,
            req.traceId(), req.spanId(), tagsText, req.host()
        ));

        if (truncated && storePayload && payload != null) {
            dao.insertPayload(id, payload);
        }

        return new LogIngestResult(id, truncated);
    }
}

