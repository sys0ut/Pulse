package com.example.pulse.partitionmanager.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * v1 skeleton:
 * - 파티션 생성/삭제는 운영 환경의 CUBRID 파티션 DDL 문법/정책에 맞게 SQL을 세팅해 실행한다.
 * - 기본값은 disabled(안전).
 */
@Component
public class PartitionMaintenanceJob {
    private static final Logger log = LoggerFactory.getLogger(PartitionMaintenanceJob.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final JdbcTemplate jdbc;
    private final boolean enabled;

    public PartitionMaintenanceJob(JdbcTemplate jdbc, @Value("${pulse.partition.enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.enabled = enabled;
    }

    @Scheduled(cron = "${pulse.partition.cron:0 5 0 * * *}")
    public void runDaily() {
        if (!enabled) {
            log.info("partition-maintenance disabled (pulse.partition.enabled=false)");
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate create1 = today.plusDays(1);
        LocalDate create2 = today.plusDays(2);
        LocalDate drop = today.minusDays(8);

        // NOTE: 아래 SQL은 '예시'입니다. CUBRID 11.4 파티션 DDL 문법에 맞게 바꾸세요.
        // 운영 반영 전, 반드시 staging에서 검증하세요.
        exec("/* TODO */", "create-partition", create1);
        exec("/* TODO */", "create-partition", create2);
        exec("/* TODO */", "drop-partition", drop);
    }

    private void exec(String sql, String action, LocalDate day) {
        String key = day.format(DAY);
        log.info("partition-maintenance action={} day={}", action, key);
        if (sql == null || sql.isBlank() || sql.contains("TODO")) {
            log.warn("partition-maintenance SQL not configured for action={} day={}", action, key);
            return;
        }
        jdbc.execute(sql);
    }
}

