package com.example.pulse.dbexporter.scrape.parse;

import java.util.ArrayList;
import java.util.List;

public final class BrokerStatusParser {
  private BrokerStatusParser() {}

  public static BrokerStatusSnapshot parse(String stdout) {
    List<BrokerStatusSnapshot.BrokerRow> rows = new ArrayList<>();
    if (stdout == null || stdout.isBlank()) {
      return new BrokerStatusSnapshot(rows);
    }

    for (String line : stdout.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.startsWith("@")
          || trimmed.startsWith("NAME")
          || trimmed.startsWith("=")
          || trimmed.startsWith("-")) {
        continue;
      }

      // Example:
      // * broker1 9451 33023 5 0 0 0 0 0 0 0 0 0/60.0 0/60.0 0 0 0 0
      String[] tokens = trimmed.split("\\s+");
      int idx = 0;
      if (tokens.length > 0 && "*".equals(tokens[0])) {
        idx = 1;
      }
      if (tokens.length - idx < 18) {
        continue;
      }

      String broker = tokens[idx];
      int port = parseInt(tokens[idx + 2]);
      double tps = parseDouble(tokens[idx + 5]);
      double qps = parseDouble(tokens[idx + 6]);

      long select = parseLong(tokens[idx + 7]);
      long insert = parseLong(tokens[idx + 8]);
      long update = parseLong(tokens[idx + 9]);
      long delete = parseLong(tokens[idx + 10]);
      long others = parseLong(tokens[idx + 11]);

      LongPair longT = parseCountThreshold(tokens[idx + 12]);
      LongPair longQ = parseCountThreshold(tokens[idx + 13]);

      long errQ = parseLong(tokens[idx + 14]);
      long uniqueErrQ = parseLong(tokens[idx + 15]);
      long connect = parseLong(tokens[idx + 16]);
      long reject = parseLong(tokens[idx + 17]);

      rows.add(
          new BrokerStatusSnapshot.BrokerRow(
              broker,
              port,
              tps,
              qps,
              select,
              insert,
              update,
              delete,
              others,
              errQ,
              uniqueErrQ,
              connect,
              reject,
              longT.count,
              longT.thresholdSeconds,
              longQ.count,
              longQ.thresholdSeconds));
    }

    return new BrokerStatusSnapshot(rows);
  }

  private static int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static long parseLong(String s) {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

  private static double parseDouble(String s) {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  private static LongPair parseCountThreshold(String s) {
    if (s == null) {
      return new LongPair(0, 0.0);
    }
    String[] parts = s.split("/");
    if (parts.length != 2) {
      return new LongPair(parseLong(s), 0.0);
    }
    return new LongPair(parseLong(parts[0]), parseDouble(parts[1]));
  }

  private record LongPair(long count, double thresholdSeconds) {}
}

