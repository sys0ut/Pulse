package com.example.pulse.storage.dao;

import com.example.pulse.common.api.LogQueryResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
@Repository
public class LogQueryDao {
    private final JdbcTemplate jdbc;

    public LogQueryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<LogQueryResponse.LogRow> query(
        String serviceName,
        Instant from,
        Instant to,
        String level,
        String traceId,
        int limit,
        Instant cursorTs,
        Long cursorId
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 2000));

        StringBuilder sql = new StringBuilder("""
            SELECT log_id, ts, service, level, message, is_truncated, trace_id, tags_text
            FROM logs
            WHERE service = ?
              AND ts >= ?
              AND ts < ?
            """);

        List<Object> args = new ArrayList<>();
        args.add(serviceName);
        args.add(Timestamp.from(from));
        args.add(Timestamp.from(to));

        if (level != null && !level.isBlank()) {
            sql.append(" AND level = ?\n");
            args.add(level);
        }
        if (traceId != null && !traceId.isBlank()) {
            sql.append(" AND trace_id = ?\n");
            args.add(traceId);
        }
        if (cursorTs != null && cursorId != null) {
            // keyset pagination (ts, id) descending
            sql.append(" AND (ts < ? OR (ts = ? AND log_id < ?))\n");
            args.add(Timestamp.from(cursorTs));
            args.add(Timestamp.from(cursorTs));
            args.add(cursorId);
        }

        sql.append(" ORDER BY ts DESC, log_id DESC LIMIT ?\n");
        args.add(safeLimit);

        return jdbc.query(
            sql.toString(),
            (rs, rowNum) -> new LogQueryResponse.LogRow(
                rs.getLong("log_id"),
                rs.getTimestamp("ts").toInstant(),
                rs.getString("service"),
                rs.getString("level"),
                rs.getString("message"),
                rs.getInt("is_truncated") != 0,
                rs.getString("trace_id"),
                rs.getString("tags_text")
            ),
            args.toArray()
        );
    }
}

