package com.example.pulse.dbexporter.scrape.parse;

import java.util.List;

public record SpaceDbSnapshot(List<SpaceSummaryRow> summaries, List<FileDetailRow> details) {
  public record SpaceSummaryRow(
      String type, String purpose, long volumeCount, double usedBytes, double freeBytes, double totalBytes) {}

  public record FileDetailRow(
      String dataType,
      long fileCount,
      double usedBytes,
      double reservedBytes,
      double totalBytes) {}
}

