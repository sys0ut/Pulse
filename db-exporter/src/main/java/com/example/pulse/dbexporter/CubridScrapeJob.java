package com.example.pulse.dbexporter;

import com.example.pulse.dbexporter.metrics.GaugeStore;
import com.example.pulse.dbexporter.scrape.CommandResult;
import com.example.pulse.dbexporter.scrape.ProcessRunner;
import com.example.pulse.dbexporter.scrape.parse.BrokerStatusParser;
import com.example.pulse.dbexporter.scrape.parse.HeartbeatParser;
import com.example.pulse.dbexporter.scrape.parse.SpaceDbParser;
import com.example.pulse.dbexporter.scrape.parse.TranlistParser;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CubridScrapeJob {

  private static final String METRIC_SCRAPE_SUCCESS = "pulse_cubrid_scrape_success";
  private static final String METRIC_SCRAPE_DURATION_SECONDS = "pulse_cubrid_scrape_duration_seconds";
  private static final String METRIC_DB_SCRAPE_SUCCESS = "pulse_cubrid_db_scrape_success";

  private final CubridExporterProperties props;
  private final ProcessRunner runner;
  private final GaugeStore gauges;

  // node -> lastState
  private final Map<String, String> lastNodeState = new ConcurrentHashMap<>();
  // (name|db) -> lastState
  private final Map<String, String> lastProcessState = new ConcurrentHashMap<>();

  public CubridScrapeJob(CubridExporterProperties props, ProcessRunner runner, MeterRegistry registry) {
    this.props = Objects.requireNonNull(props);
    this.runner = Objects.requireNonNull(runner);
    this.gauges = new GaugeStore(registry);
  }

  @Scheduled(fixedDelayString = "${cubrid.scrape-interval:14s}")
  public void scrape() {
    scrapeBrokerStatus();
    scrapeHeartbeatStatus();

    List<String> dbs = props.dbList();
    if (dbs == null || dbs.isEmpty()) {
      return;
    }
    for (String rawDb : dbs) {
      String db = rawDb == null ? "" : rawDb.trim();
      if (db.isEmpty()) {
        continue;
      }
      scrapeTranlist(db);
      scrapeSpaceDb(db);
    }
  }

  private void scrapeBrokerStatus() {
    long startNanos = System.nanoTime();
    boolean ok = false;
    try {
      CommandResult result = runner.run(command("broker", "status", "-b"), props.commandTimeout());
      ok = result.success();
      if (ok) {
        var snapshot = BrokerStatusParser.parse(result.stdout());
        for (var row : snapshot.brokers()) {
          Tags tags = Tags.of("broker", row.broker(), "port", String.valueOf(row.port()));
          gauges.set("pulse_cubrid_broker_tps", tags, row.tps());
          gauges.set("pulse_cubrid_broker_qps", tags, row.qps());

          gauges.set("pulse_cubrid_broker_stmt_select", tags, row.selectCount());
          gauges.set("pulse_cubrid_broker_stmt_insert", tags, row.insertCount());
          gauges.set("pulse_cubrid_broker_stmt_update", tags, row.updateCount());
          gauges.set("pulse_cubrid_broker_stmt_delete", tags, row.deleteCount());
          gauges.set("pulse_cubrid_broker_stmt_others", tags, row.othersCount());

          gauges.set("pulse_cubrid_broker_err_query_count", tags, row.errQueryCount());
          gauges.set("pulse_cubrid_broker_unique_err_query_count", tags, row.uniqueErrQueryCount());
          gauges.set("pulse_cubrid_broker_connect_count", tags, row.connectCount());
          gauges.set("pulse_cubrid_broker_reject_count", tags, row.rejectCount());

          gauges.set("pulse_cubrid_broker_long_transaction_count", tags, row.longTranCount());
          gauges.set(
              "pulse_cubrid_broker_long_threshold_seconds",
              tags.and("type", "tran"),
              row.longTranThresholdSeconds());

          gauges.set("pulse_cubrid_broker_long_query_count", tags, row.longQueryCount());
          gauges.set(
              "pulse_cubrid_broker_long_threshold_seconds",
              tags.and("type", "query"),
              row.longQueryThresholdSeconds());
        }
      }
    } catch (Exception ignored) {
      ok = false;
    } finally {
      finishSource("broker", ok, startNanos);
    }
  }

  private void scrapeTranlist(String db) {
    long startNanos = System.nanoTime();
    boolean ok = false;
    try {
      CommandResult result = runner.run(command("tranlist", db), props.commandTimeout());
      ok = result.success();
      gauges.set(METRIC_DB_SCRAPE_SUCCESS, Tags.of("source", "tranlist", "db", db), ok ? 1 : 0);
      if (ok) {
        var snap = TranlistParser.parse(result.stdout());
        Tags tags = Tags.of("db", db);
        gauges.set("pulse_cubrid_tran_sessions", tags, snap.sessions());
        gauges.set("pulse_cubrid_tran_active", tags, snap.activeTransactions());
        gauges.set("pulse_cubrid_tran_lock_wait", tags, snap.lockWaitTransactions());
        gauges.set("pulse_cubrid_tran_longest_query_seconds", tags, snap.longestQuerySeconds());
        gauges.set("pulse_cubrid_tran_longest_tran_seconds", tags, snap.longestTranSeconds());
      }
    } catch (Exception ignored) {
      ok = false;
      gauges.set(METRIC_DB_SCRAPE_SUCCESS, Tags.of("source", "tranlist", "db", db), 0);
    } finally {
      finishSource("tranlist", ok, startNanos);
    }
  }

  private void scrapeSpaceDb(String db) {
    long startNanos = System.nanoTime();
    boolean ok = false;
    try {
      CommandResult result = runner.run(command("spacedb", "-sp", db), props.commandTimeout());
      ok = result.success();
      gauges.set(METRIC_DB_SCRAPE_SUCCESS, Tags.of("source", "spacedb", "db", db), ok ? 1 : 0);
      if (ok) {
        var snap = SpaceDbParser.parse(result.stdout());
        for (var row : snap.summaries()) {
          Tags tags = Tags.of("db", db, "type", row.type(), "purpose", row.purpose());
          gauges.set("pulse_cubrid_space_used_bytes", tags, row.usedBytes());
          gauges.set("pulse_cubrid_space_free_bytes", tags, row.freeBytes());
          gauges.set("pulse_cubrid_space_total_bytes", tags, row.totalBytes());
          gauges.set("pulse_cubrid_space_volume_count", tags, row.volumeCount());
        }
        for (var row : snap.details()) {
          Tags tags = Tags.of("db", db, "data_type", row.dataType());
          gauges.set("pulse_cubrid_file_used_bytes", tags, row.usedBytes());
          gauges.set("pulse_cubrid_file_reserved_bytes", tags, row.reservedBytes());
          gauges.set("pulse_cubrid_file_total_bytes", tags, row.totalBytes());
          gauges.set("pulse_cubrid_file_count", tags, row.fileCount());
        }
      }
    } catch (Exception ignored) {
      ok = false;
      gauges.set(METRIC_DB_SCRAPE_SUCCESS, Tags.of("source", "spacedb", "db", db), 0);
    } finally {
      finishSource("spacedb", ok, startNanos);
    }
  }

  private void scrapeHeartbeatStatus() {
    long startNanos = System.nanoTime();
    boolean ok = false;
    try {
      CommandResult result = runner.run(command("heartbeat", "status"), props.commandTimeout());
      ok = result.success();
      if (ok) {
        var snap = HeartbeatParser.parse(result.stdout());
        // peer nodes (excluding current)
        long peers = Math.max(0, snap.nodes().size() - 1);
        gauges.set("pulse_cubrid_ha_peer_nodes", Tags.empty(), peers);

        for (var node : snap.nodes()) {
          updateNodeState(node.node(), node.state());
        }
        // current node may not always appear in node list; ensure it's included.
        if (snap.currentNode() != null && !snap.currentNode().isBlank()) {
          updateNodeState(snap.currentNode(), snap.currentState());
        }

        for (var proc : snap.processes()) {
          updateProcessState(proc.name(), proc.db(), proc.state());
        }
      }
    } catch (Exception ignored) {
      ok = false;
    } finally {
      finishSource("heartbeat", ok, startNanos);
    }
  }

  private void updateNodeState(String node, String state) {
    if (node == null || node.isBlank() || state == null || state.isBlank()) {
      return;
    }
    String normalizedState = state.trim().toLowerCase(Locale.ROOT);
    String key = node.trim();

    String prev = lastNodeState.put(key, normalizedState);
    if (prev != null && !prev.equals(normalizedState)) {
      gauges.set("pulse_cubrid_ha_node_state", Tags.of("node", key, "state", prev), 0);
    }

    gauges.set("pulse_cubrid_ha_node_state", Tags.of("node", key, "state", normalizedState), 1);
    gauges.set("pulse_cubrid_ha_role", Tags.of("node", key), "master".equals(normalizedState) ? 1 : 0);
  }

  private void updateProcessState(String name, String db, String state) {
    if (name == null || name.isBlank() || state == null || state.isBlank()) {
      return;
    }
    String n = name.trim().toLowerCase(Locale.ROOT);
    String d = db == null ? "" : db.trim();
    String s = state.trim().toLowerCase(Locale.ROOT);

    String id = n + "|" + d;
    String prev = lastProcessState.put(id, s);
    if (prev != null && !prev.equals(s)) {
      gauges.set("pulse_cubrid_ha_process_state", processTags(n, d, prev), 0);
    }
    gauges.set("pulse_cubrid_ha_process_state", processTags(n, d, s), 1);
  }

  private static Tags processTags(String name, String db, String state) {
    List<String> tags = new ArrayList<>();
    tags.add("name");
    tags.add(name);
    tags.add("state");
    tags.add(state);
    if (db != null && !db.isBlank()) {
      tags.add("db");
      tags.add(db);
    }
    return Tags.of(tags.toArray(new String[0]));
  }

  private void finishSource(String source, boolean ok, long startNanos) {
    double durationSeconds = (System.nanoTime() - startNanos) / 1_000_000_000.0;
    gauges.set(METRIC_SCRAPE_SUCCESS, Tags.of("source", source), ok ? 1 : 0);
    gauges.set(METRIC_SCRAPE_DURATION_SECONDS, Tags.of("source", source), durationSeconds);
  }

  private List<String> command(String... args) {
    List<String> cmd = new ArrayList<>();
    cmd.add(props.bin());
    for (String a : args) {
      cmd.add(a);
    }
    return cmd;
  }
}

