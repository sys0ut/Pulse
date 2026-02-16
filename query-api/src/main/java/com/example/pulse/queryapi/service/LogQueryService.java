package com.example.pulse.queryapi.service;

import com.example.pulse.common.api.LogQueryResponse;
import com.example.pulse.storage.dao.LogQueryDao;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LogQueryService {
    private final LogQueryDao dao;

    public LogQueryService(LogQueryDao dao) {
        this.dao = dao;
    }

    public LogQueryResponse query(
        String serviceName,
        Instant from,
        Instant to,
        String level,
        String traceId,
        int limit,
        Instant cursorTs,
        Long cursorId
    ) {
        List<LogQueryResponse.LogRow> rows = dao.query(serviceName, from, to, level, traceId, limit, cursorTs, cursorId);
        if (rows.isEmpty()) {
            return new LogQueryResponse(rows, null, null);
        }
        LogQueryResponse.LogRow last = rows.get(rows.size() - 1);
        return new LogQueryResponse(rows, last.ts(), last.logId());
    }
}

