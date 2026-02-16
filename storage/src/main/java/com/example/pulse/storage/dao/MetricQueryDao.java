package com.example.pulse.storage.dao;

import com.example.pulse.common.api.MetricTimeSeriesResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Repository
public class MetricQueryDao {
    private final JdbcTemplate jdbc;

    public MetricQueryDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public MetricTimeSeriesResponse timeseries(String serviceName, String name, Instant from, Instant to, String tagsKey) {
        String sql = """
            SELECT bucket_ts, cnt, sum_v, min_v, max_v
            FROM metric_rollup_1m
            WHERE service = ?
              AND name = ?
              AND tags_key = ?
              AND bucket_ts >= ?
              AND bucket_ts < ?
            ORDER BY bucket_ts ASC
            """;

        List<MetricTimeSeriesResponse.Point> points = jdbc.query(
            sql,
            (rs, rowNum) -> {
                Instant t = rs.getTimestamp("bucket_ts").toInstant();
                long cnt = rs.getLong("cnt");
                double sum = rs.getDouble("sum_v");
                double avg = cnt == 0 ? 0.0 : sum / cnt;
                double min = rs.getDouble("min_v");
                double max = rs.getDouble("max_v");
                return new MetricTimeSeriesResponse.Point(t, avg, min, max, cnt);
            },
            serviceName,
            name,
            tagsKey == null || tagsKey.isBlank() ? "-" : tagsKey,
            Timestamp.from(from),
            Timestamp.from(to)
        );

        return new MetricTimeSeriesResponse(serviceName, name, tagsKey == null || tagsKey.isBlank() ? "-" : tagsKey, 60, points);
    }
}

