package com.example.pulse.storage.dao;

import com.example.pulse.storage.model.LogWriteRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LogWriteDao {
    private final JdbcTemplate jdbc;

    public LogWriteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insertLog(LogWriteRequest r) {
        // v1: 테이블명은 고정(파티션은 DB에서 관리)
        String sql = """
            INSERT INTO logs
              (log_id, ts, service, level, message, is_truncated, full_len, trace_id, span_id, tags_text, host)
            VALUES
              (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        return jdbc.update(
            sql,
            r.logId(),
            java.sql.Timestamp.from(r.ts()),
            r.service(),
            r.level(),
            r.message(),
            r.truncated() ? 1 : 0,
            r.fullLen(),
            r.traceId(),
            r.spanId(),
            r.tagsText(),
            r.host()
        );
    }

    public int insertPayload(long logId, String payload) {
        String sql = """
            INSERT INTO log_payload (log_id, payload)
            VALUES (?, ?)
            """;
        return jdbc.update(sql, logId, payload);
    }
}

