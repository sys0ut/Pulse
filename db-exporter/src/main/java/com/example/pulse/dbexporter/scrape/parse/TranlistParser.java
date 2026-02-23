package com.example.pulse.dbexporter.scrape.parse;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TranlistParser {
  private TranlistParser() {}

  // Example:
  // 1(ACTIVE) DBA host 13049 csql 0.00 0.00 -1 *** empty ***
  private static final Pattern ROW =
      Pattern.compile(
          "^\\s*(\\d+)\\(([^)]+)\\)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+)\\s+(\\S+)\\s+(\\S+)\\s+(\\d+(?:\\.\\d+)?)\\s+(\\d+(?:\\.\\d+)?)\\s+(-?\\d+)\\s+.*$");

  public static TranlistSnapshot parse(String stdout) {
    if (stdout == null || stdout.isBlank()) {
      return new TranlistSnapshot(0, 0, 0, 0.0, 0.0);
    }

    long sessions = 0;
    long active = 0;
    long lockWait = 0;
    double maxQuery = 0.0;
    double maxTran = 0.0;

    for (String line : stdout.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.startsWith("Tran index") || trimmed.startsWith("-")) {
        continue;
      }

      Matcher m = ROW.matcher(trimmed);
      if (!m.matches()) {
        continue;
      }

      sessions++;
      String state = m.group(2);
      if ("ACTIVE".equalsIgnoreCase(state)) {
        active++;
      }

      double queryTime = parseDouble(m.group(8));
      double tranTime = parseDouble(m.group(9));
      int lockHolder = parseInt(m.group(10));
      if (lockHolder != -1) {
        lockWait++;
      }

      if (queryTime > maxQuery) {
        maxQuery = queryTime;
      }
      if (tranTime > maxTran) {
        maxTran = tranTime;
      }
    }

    return new TranlistSnapshot(sessions, active, lockWait, maxQuery, maxTran);
  }

  private static double parseDouble(String s) {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  private static int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return -1;
    }
  }
}

