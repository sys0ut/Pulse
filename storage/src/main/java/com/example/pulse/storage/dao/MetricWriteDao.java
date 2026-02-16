package com.example.pulse.storage.dao;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class MetricWriteDao {
    private final JdbcTemplate jdbc;

    public MetricWriteDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public int insertPoint(
        long pointId,
        Instant ts,
        String service,
        String name,
        String type,
        double value,
        String tagsText
    ) {
        String sql = """
            INSERT INTO metric_points
              (point_id, ts, service, name, mtype, value, tags_text)
            VALUES
              (?, ?, ?, ?, ?, ?, ?)
            """;
        return jdbc.update(sql, pointId, Timestamp.from(ts), service, name, type, value, tagsText);
    }

    /**
     * v1 upsert: UPDATE 먼저 시도 후 없으면 INSERT.
     * - 경쟁 조건에서 INSERT가 충돌하면 DuplicateKeyException을 잡아 UPDATE 재시도.
     */
    public boolean upsertRollup1m(
        Instant bucketTs,
        String service,
        String name,
        String tagsKey,
        double value
    ) {
        int updated = updateRollup(bucketTs, service, name, tagsKey, value);
        if (updated > 0) return true;

        try {
            insertRollup(bucketTs, service, name, tagsKey, value);
            return true;
        } catch (DuplicateKeyException e) {
            return updateRollup(bucketTs, service, name, tagsKey, value) > 0;
        }
    }

    private int updateRollup(Instant bucketTs, String service, String name, String tagsKey, double value) {
        String sql = """
            UPDATE metric_rollup_1m
            SET
              cnt = cnt + 1,
              sum_v = sum_v + ?,
              min_v = CASE WHEN min_v > ? THEN ? ELSE min_v END,
              max_v = CASE WHEN max_v < ? THEN ? ELSE max_v END
            WHERE bucket_ts = ?
              AND service = ?
              AND name = ?
              AND tags_key = ?
            """;
        return jdbc.update(
            sql,
            value,
            value, value,
            value, value,
            Timestamp.from(bucketTs),
            service,
            name,
            tagsKey
        );
    }

    private int insertRollup(Instant bucketTs, String service, String name, String tagsKey, double value) {
        String sql = """
            INSERT INTO metric_rollup_1m
              (bucket_ts, service, name, tags_key, cnt, sum_v, min_v, max_v)
            VALUES
              (?, ?, ?, ?, 1, ?, ?, ?)
            """;
        return jdbc.update(
            sql,
            Timestamp.from(bucketTs),
            service,
            name,
            tagsKey,
            value,
            value,
            value
        );
    }
}

