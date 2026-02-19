package com.example.pulse.queryapi.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class MetricRollupKey implements Serializable {
    @Column(name = "bucket_ts", nullable = false)
    private Instant bucketTs;

    @Column(name = "service", nullable = false, length = 64)
    private String service;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "tags_key", nullable = false, length = 64)
    private String tagsKey;

    protected MetricRollupKey() {}

    public MetricRollupKey(Instant bucketTs, String service, String name, String tagsKey) {
        this.bucketTs = bucketTs;
        this.service = service;
        this.name = name;
        this.tagsKey = tagsKey;
    }

    public Instant getBucketTs() {
        return bucketTs;
    }

    public String getService() {
        return service;
    }

    public String getName() {
        return name;
    }

    public String getTagsKey() {
        return tagsKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricRollupKey that)) return false;
        return Objects.equals(bucketTs, that.bucketTs)
            && Objects.equals(service, that.service)
            && Objects.equals(name, that.name)
            && Objects.equals(tagsKey, that.tagsKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucketTs, service, name, tagsKey);
    }
}

