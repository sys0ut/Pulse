package com.example.pulse.partitionmanager.service;

import com.example.pulse.partitionmanager.config.PartitionMaintenanceProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Skeleton scheduler: runs only when {@code pulse.partition.enabled=true} and SQL templates are set.
 * <p>
 * Configure CUBRID-compatible DDL in {@code application.yml} (or env). Replace {@code {day}} with
 * the target partition key ({@code yyyyMMdd}, UTC). Validate in staging before production.
 */
@Component
public class PartitionMaintenanceJob {
    private static final Logger log = LoggerFactory.getLogger(PartitionMaintenanceJob.class);
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE; // yyyyMMdd

    private final JdbcTemplate jdbc;
    private final PartitionMaintenanceProperties props;

    public PartitionMaintenanceJob(JdbcTemplate jdbc, PartitionMaintenanceProperties props) {
        this.jdbc = jdbc;
        this.props = props;
    }

    @Scheduled(cron = "${pulse.partition.cron:0 5 0 * * *}")
    public void runDaily() {
        if (!props.enabled()) {
            log.info("partition-maintenance disabled (pulse.partition.enabled=false)");
            return;
        }

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate create1 = today.plusDays(1);
        LocalDate create2 = today.plusDays(2);
        LocalDate drop = today.minusDays(8);

        exec(props.createSqlTemplate(), "create-partition+1d", create1);
        exec(props.createSqlTemplate(), "create-partition+2d", create2);
        exec(props.dropSqlTemplate(), "drop-partition-8d", drop);
    }

    private void exec(String sqlTemplate, String action, LocalDate day) {
        String key = day.format(DAY);
        log.info("partition-maintenance action={} day={}", action, key);
        if (sqlTemplate == null || sqlTemplate.isBlank()) {
            log.warn("partition-maintenance SQL template not configured for action={} day={}", action, key);
            return;
        }
        String sql = sqlTemplate.replace("{day}", key);
        jdbc.execute(sql);
    }
}
