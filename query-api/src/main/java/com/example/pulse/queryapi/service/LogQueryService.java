package com.example.pulse.queryapi.service;

import com.example.pulse.common.api.LogQueryResponse;
import com.example.pulse.queryapi.persistence.entity.LogEntity;
import com.example.pulse.queryapi.persistence.repo.LogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class LogQueryService {
    private final LogRepository repo;

    public LogQueryService(LogRepository repo) {
        this.repo = repo;
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
        int safeLimit = Math.max(1, Math.min(limit, 2000));

        Specification<LogEntity> spec = (root, q, cb) -> {
            var p = cb.conjunction();

            p.getExpressions().add(cb.equal(root.get("service"), serviceName));
            p.getExpressions().add(cb.greaterThanOrEqualTo(root.get("ts"), from));
            p.getExpressions().add(cb.lessThan(root.get("ts"), to));

            if (level != null && !level.isBlank()) {
                p.getExpressions().add(cb.equal(root.get("level"), level));
            }
            if (traceId != null && !traceId.isBlank()) {
                p.getExpressions().add(cb.equal(root.get("traceId"), traceId));
            }
            if (cursorTs != null && cursorId != null) {
                // keyset pagination for DESC order (ts, logId)
                var tsPath = root.get("ts").as(Instant.class);
                var idPath = root.get("logId").as(Long.class);
                var cursor = cb.or(
                    cb.lessThan(tsPath, cursorTs),
                    cb.and(cb.equal(tsPath, cursorTs), cb.lessThan(idPath, cursorId))
                );
                p.getExpressions().add(cursor);
            }
            return p;
        };

        var pageable = PageRequest.of(
            0,
            safeLimit,
            Sort.by(Sort.Order.desc("ts"), Sort.Order.desc("logId"))
        );

        List<LogQueryResponse.LogRow> rows = repo.findAll(spec, pageable)
            .getContent()
            .stream()
            .map(e -> new LogQueryResponse.LogRow(
                e.getLogId(),
                e.getTs(),
                e.getService(),
                e.getLevel(),
                e.getMessage(),
                e.isTruncated(),
                e.getTraceId(),
                e.getTagsText()
            ))
            .toList();

        if (rows.isEmpty()) {
            return new LogQueryResponse(rows, null, null);
        }
        LogQueryResponse.LogRow last = rows.get(rows.size() - 1);
        return new LogQueryResponse(rows, last.ts(), last.logId());
    }
}

