package com.example.pulse.queryapi.persistence.repo;

import com.example.pulse.queryapi.persistence.entity.MetricRollup1mEntity;
import com.example.pulse.queryapi.persistence.entity.MetricRollupKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface MetricRollup1mRepository extends JpaRepository<MetricRollup1mEntity, MetricRollupKey> {

    @Query("""
        SELECT r
        FROM MetricRollup1mEntity r
        WHERE r.id.service = :service
          AND r.id.name = :name
          AND r.id.tagsKey = :tagsKey
          AND r.id.bucketTs >= :from
          AND r.id.bucketTs < :to
        ORDER BY r.id.bucketTs ASC
        """)
    List<MetricRollup1mEntity> range(
        @Param("service") String service,
        @Param("name") String name,
        @Param("tagsKey") String tagsKey,
        @Param("from") Instant from,
        @Param("to") Instant to
    );
}

