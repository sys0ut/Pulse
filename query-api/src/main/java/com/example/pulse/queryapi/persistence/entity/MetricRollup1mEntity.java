package com.example.pulse.queryapi.persistence.entity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "metric_rollup_1m")
@Access(AccessType.FIELD)
public class MetricRollup1mEntity {

    @EmbeddedId
    private MetricRollupKey id;

    @Column(name = "cnt", nullable = false)
    private Long cnt;

    @Column(name = "sum_v", nullable = false)
    private Double sumV;

    @Column(name = "min_v", nullable = false)
    private Double minV;

    @Column(name = "max_v", nullable = false)
    private Double maxV;

    protected MetricRollup1mEntity() {}

    public MetricRollupKey getId() {
        return id;
    }

    public long getCnt() {
        return cnt == null ? 0L : cnt;
    }

    public double getSumV() {
        return sumV == null ? 0.0 : sumV;
    }

    public double getMinV() {
        return minV == null ? 0.0 : minV;
    }

    public double getMaxV() {
        return maxV == null ? 0.0 : maxV;
    }
}

