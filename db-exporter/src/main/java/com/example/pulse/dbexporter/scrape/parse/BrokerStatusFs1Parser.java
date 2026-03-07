package com.example.pulse.dbexporter.scrape.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Parser for: {@code cubrid broker status -b -f -s <seconds>}.
 *
 * <p>This command may print repeated snapshots; we parse all broker rows that appear in stdout.
 */
public final class BrokerStatusFs1Parser {
  private BrokerStatusFs1Parser() {}

  // ANSI CSI / OSC sequences, plus stray control characters.
  // We sometimes run the command under `script` (pseudo-tty), which can introduce terminal control
  // sequences (e.g. ESC[H) into stdout.
  private static final Pattern ANSI_ESCAPE =
      Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]|\u001B\\][^\u001B]*\u001B\\\\|\u001B[@-_]");

  public static BrokerStatusFs1Snapshot parse(String stdout) {
    List<BrokerStatusFs1Snapshot.BrokerRow> rows = new ArrayList<>();
    if (stdout == null || stdout.isBlank()) {
      return new BrokerStatusFs1Snapshot(rows);
    }

    for (String line : stdout.split("\n")) {
      String trimmed = sanitize(line);
      if (trimmed.isEmpty()) {
        continue;
      }

      // In "-f -s" mode, the output may contain multiple snapshots.
      // When we see a new header line, we treat it as the beginning of a new snapshot and
      // keep only the latest snapshot rows.
      if (trimmed.startsWith("NAME")) {
        rows.clear();
        continue;
      }

      if (trimmed.startsWith("@") || trimmed.startsWith("=") || trimmed.startsWith("-")) {
        continue;
      }

      // Example (wrapped spacing):
      // * broker1 935 220776 33000 5 0 0 0 0 0 0 0 0/60.0 0/60.0 0 0 0 RW ALL 54630 0
      String[] tokens = trimmed.split("\\s+");
      int idx = 0;
      if (tokens.length > 0 && "*".equals(tokens[0])) {
        idx = 1;
      }
      // Minimal columns we need:
      // NAME PID PSIZE PORT AS(T W B 1s-W 1s-B) JQ TPS QPS ...
      if (tokens.length - idx < 12) {
        continue;
      }

      String broker = tokens[idx];
      // Filter out terminal artifacts like "[H" that can appear when stdout includes control codes.
      // Broker names are expected to be simple identifiers (e.g., broker1, query_editor).
      if (!broker.matches("[A-Za-z0-9_]+")) {
        continue;
      }
      int port = parseInt(tokens[idx + 3]);
      double tps = parseDouble(tokens[idx + 10]);
      double qps = parseDouble(tokens[idx + 11]);

      rows.add(new BrokerStatusFs1Snapshot.BrokerRow(broker, port, tps, qps));
    }

    return new BrokerStatusFs1Snapshot(rows);
  }

  private static String sanitize(String line) {
    if (line == null) {
      return "";
    }
    // Remove ANSI escape sequences and other non-printable control characters.
    String s = ANSI_ESCAPE.matcher(line).replaceAll("");
    s = s.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "");
    return s.trim();
  }

  private static int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static double parseDouble(String s) {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }
}

