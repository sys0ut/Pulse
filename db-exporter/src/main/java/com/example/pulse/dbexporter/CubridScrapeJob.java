package com.example.pulse.dbexporter;

import com.example.pulse.dbexporter.metrics.GaugeStore;
import com.example.pulse.dbexporter.scrape.CommandResult;
import com.example.pulse.dbexporter.scrape.ProcessRunner;
import com.example.pulse.dbexporter.scrape.parse.BrokerStatusFs1Parser;
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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CubridScrapeJob {

  private static final Logger log = LoggerFactory.getLogger(CubridScrapeJob.class);

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

  @Scheduled(fixedDelayString = "${cubrid.broker-fs1-scrape-interval:1s}")
  public void scrapeBrokerStatusFs1() {
    if (!props.brokerFs1Enabled()) {
      return;
    }

    long startNanos = System.nanoTime();
    boolean ok = false;
    try {
      // `-f -s 1` prints an initial snapshot (often cumulative since broker start),
      // then after ~1s starts printing 1s-rate snapshots. We capture long enough to include
      // at least 2 snapshots and parse the last one.
      CommandResult result =
          runner.runForCapture(
              commandForFs1Capture(
                  "broker",
                  "status",
                  "-b",
                  "-f",
                  "-s",
                  String.valueOf(props.brokerFs1SampleSeconds())),
              props.brokerFs1CaptureDuration());

      var snapshot = BrokerStatusFs1Parser.parse(result.stdout());
      ok = !snapshot.brokers().isEmpty();
      if (ok) {
        for (var row : snapshot.brokers()) {
          Tags tags = Tags.of("broker", row.broker(), "port", String.valueOf(row.port()));
          gauges.set("pulse_cubrid_broker_tps_1s", tags, row.tps());
          gauges.set("pulse_cubrid_broker_qps_1s", tags, row.qps());
        }
      } else {
        String out = result.stdout();
        String err = result.stderr();
        String outSnippet = snippet(out);
        String errSnippet = snippet(err);
        log.warn(
            "broker_fs1 parsed empty snapshot (stdoutBytes={}, stderrBytes={}, timedOut={}, exitCode={}, stdoutSnippet='{}', stderrSnippet='{}')",
            out == null ? 0 : out.length(),
            err == null ? 0 : err.length(),
            result.timedOut(),
            result.exitCode(),
            outSnippet,
            errSnippet);
      }
    } catch (Exception e) {
      ok = false;
      log.warn("scrape source=broker_fs1 failed", e);
    } finally {
      finishSource("broker_fs1", ok, startNanos);
    }
  }

  private void scrapeBrokerStatus() {
    long startNanos = System.nanoTime();
    boolean ok = false;
    try {
      CommandResult result = runner.run(command("broker", "status", "-b"), props.commandTimeout());
      ok = result.success();
      if (!ok) {
        logScrapeCommandFailure("broker", null, result);
      } else {
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
    } catch (Exception e) {
      ok = false;
      log.warn("scrape source=broker failed", e);
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
      if (!ok) {
        logScrapeCommandFailure("tranlist", db, result);
      } else {
        var snap = TranlistParser.parse(result.stdout());
        Tags tags = Tags.of("db", db);
        gauges.set("pulse_cubrid_tran_sessions", tags, snap.sessions());
        gauges.set("pulse_cubrid_tran_active", tags, snap.activeTransactions());
        gauges.set("pulse_cubrid_tran_lock_wait", tags, snap.lockWaitTransactions());
        gauges.set("pulse_cubrid_tran_longest_query_seconds", tags, snap.longestQuerySeconds());
        gauges.set("pulse_cubrid_tran_longest_tran_seconds", tags, snap.longestTranSeconds());
      }
    } catch (Exception e) {
      ok = false;
      gauges.set(METRIC_DB_SCRAPE_SUCCESS, Tags.of("source", "tranlist", "db", db), 0);
      log.warn("scrape source=tranlist db={} failed", db, e);
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
      if (!ok) {
        logScrapeCommandFailure("spacedb", db, result);
      } else {
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
    } catch (Exception e) {
      ok = false;
      gauges.set(METRIC_DB_SCRAPE_SUCCESS, Tags.of("source", "spacedb", "db", db), 0);
      log.warn("scrape source=spacedb db={} failed", db, e);
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
      if (!ok) {
        logScrapeCommandFailure("heartbeat", null, result);
      } else {
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
    } catch (Exception e) {
      ok = false;
      log.warn("scrape source=heartbeat failed", e);
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
    // Always include the same tag keys for a given meter name.
    // Prometheus/Micrometer requires tag key sets to be consistent across registrations.
    tags.add("db");
    tags.add(db == null ? "" : db);
    return Tags.of(tags.toArray(new String[0]));
  }

  private static String snippet(String s) {
    if (s == null) {
      return "";
    }
    String t = s.replace("\r", "").replace("\n", "\\n").trim();
    if (t.length() <= 200) {
      return t;
    }
    return t.substring(0, 200) + "...";
  }

  private void logScrapeCommandFailure(String source, String db, CommandResult result) {
    String err = result.stderr();
    String out = result.stdout();
    if (db == null || db.isBlank()) {
      log.warn(
          "scrape source={} command failed (exitCode={}, timedOut={}, stderrSnippet='{}', stdoutSnippet='{}')",
          source,
          result.exitCode(),
          result.timedOut(),
          snippet(err),
          snippet(out));
      return;
    }
    log.warn(
        "scrape source={} db={} command failed (exitCode={}, timedOut={}, stderrSnippet='{}', stdoutSnippet='{}')",
        source,
        db,
        result.exitCode(),
        result.timedOut(),
        snippet(err),
        snippet(out));
  }

  /**
   * `cubrid broker status -b -f -s 1` uses a terminal UI internally on some environments.
   * When executed without a TTY (like from ProcessBuilder with piped stdout), it may fail with
   * "Cannot set terminal ... fail to initialize tinfo library".
   *
   * <p>We wrap the command with `script -q -c ... /dev/null` to provide a pseudo-tty.
   * If `script` is not available, it will fall back to the plain command.
   */
  private List<String> commandForFs1Capture(String... args) {
    List<String> base = command(args);
    String cmdString =
        base.stream().map(CubridScrapeJob::shellQuote).collect(Collectors.joining(" "));

    // util-linux `script` provides a pseudo-tty and captures the output.
    // script -q -c "<cmd>" /dev/null
    List<String> wrapped = new ArrayList<>();
    wrapped.add("script");
    wrapped.add("-q");
    wrapped.add("-c");
    wrapped.add(cmdString);
    wrapped.add("/dev/null");
    return wrapped;
  }

  private static String shellQuote(String s) {
    if (s == null) {
      return "''";
    }
    // POSIX-safe single-quote escaping: ' -> '\''
    return "'" + s.replace("'", "'\"'\"'") + "'";
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

