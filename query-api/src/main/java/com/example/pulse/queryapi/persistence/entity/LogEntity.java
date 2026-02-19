package com.example.pulse.queryapi.persistence.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "logs")
@Access(AccessType.FIELD)
public class LogEntity {

    @Id
    @Column(name = "log_id", nullable = false)
    private Long logId;

    @Column(name = "ts", nullable = false)
    private Instant ts;

    @Column(name = "service", nullable = false, length = 64)
    private String service;

    @Column(name = "level", nullable = false, length = 16)
    private String level;

    @Column(name = "message", nullable = false, length = 8192)
    private String message;

    // Use numeric representation to avoid DB/driver boolean mapping quirks.
    @Column(name = "is_truncated", nullable = false)
    private Integer isTruncated;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "tags_text", length = 4096)
    private String tagsText;

    protected LogEntity() {}

    public Long getLogId() {
        return logId;
    }

    public Instant getTs() {
        return ts;
    }

    public String getService() {
        return service;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public boolean isTruncated() {
        return isTruncated != null && isTruncated != 0;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getTagsText() {
        return tagsText;
    }
}

