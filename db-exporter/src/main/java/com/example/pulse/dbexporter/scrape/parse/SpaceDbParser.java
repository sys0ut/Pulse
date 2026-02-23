package com.example.pulse.dbexporter.scrape.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpaceDbParser {
  private SpaceDbParser() {}

  private static final Pattern SUMMARY_ROW =
      Pattern.compile(
          "^(\\S+)\\s+(.*?)\\s+(\\d+)\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s*$");

  private static final Pattern DETAIL_ROW =
      Pattern.compile(
          "^(\\S+)\\s+(\\d+)\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s+(\\d+(?:\\.\\d+)?)\\s*([KMG])\\s*$");

  public static SpaceDbSnapshot parse(String stdout) {
    List<SpaceDbSnapshot.SpaceSummaryRow> summaries = new ArrayList<>();
    List<SpaceDbSnapshot.FileDetailRow> details = new ArrayList<>();
    if (stdout == null || stdout.isBlank()) {
      return new SpaceDbSnapshot(summaries, details);
    }

    boolean inDetail = false;

    for (String line : stdout.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      if (trimmed.startsWith("Space description")
          || trimmed.startsWith("type")
          || trimmed.startsWith("Detailed space description")
          || trimmed.startsWith("data_type")
          || trimmed.startsWith("-")) {
        if (trimmed.startsWith("data_type")) {
          inDetail = true;
        }
        continue;
      }

      if (!inDetail) {
        Matcher m = SUMMARY_ROW.matcher(trimmed);
        if (!m.matches()) {
          continue;
        }
        String type = m.group(1);
        String purpose = m.group(2).trim();
        long volumeCount = parseLong(m.group(3));
        double used = toBytes(parseDouble(m.group(4)), m.group(5));
        double free = toBytes(parseDouble(m.group(6)), m.group(7));
        double total = toBytes(parseDouble(m.group(8)), m.group(9));
        summaries.add(new SpaceDbSnapshot.SpaceSummaryRow(type, purpose, volumeCount, used, free, total));
      } else {
        Matcher m = DETAIL_ROW.matcher(trimmed);
        if (!m.matches()) {
          continue;
        }
        String dataType = m.group(1);
        long fileCount = parseLong(m.group(2));
        double used = toBytes(parseDouble(m.group(3)), m.group(4));
        double reserved = toBytes(parseDouble(m.group(7)), m.group(8));
        double total = toBytes(parseDouble(m.group(9)), m.group(10));
        details.add(new SpaceDbSnapshot.FileDetailRow(dataType, fileCount, used, reserved, total));
      }
    }

    return new SpaceDbSnapshot(summaries, details);
  }

  private static double toBytes(double value, String unit) {
    String u = unit.toUpperCase(Locale.ROOT);
    return switch (u) {
      case "K" -> value * 1024.0;
      case "M" -> value * 1024.0 * 1024.0;
      case "G" -> value * 1024.0 * 1024.0 * 1024.0;
      default -> value;
    };
  }

  private static double parseDouble(String s) {
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException e) {
      return 0.0;
    }
  }

  private static long parseLong(String s) {
    try {
      return Long.parseLong(s);
    } catch (NumberFormatException e) {
      return 0L;
    }
  }
}

