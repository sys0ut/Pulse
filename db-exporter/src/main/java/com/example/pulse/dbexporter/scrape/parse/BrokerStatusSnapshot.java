package com.example.pulse.dbexporter.scrape.parse;

import java.util.List;

public record BrokerStatusSnapshot(List<BrokerRow> brokers) {
  public record BrokerRow(
      String broker,
      int port,
      double tps,
      double qps,
      long selectCount,
      long insertCount,
      long updateCount,
      long deleteCount,
      long othersCount,
      long errQueryCount,
      long uniqueErrQueryCount,
      long connectCount,
      long rejectCount,
      long longTranCount,
      double longTranThresholdSeconds,
      long longQueryCount,
      double longQueryThresholdSeconds) {}
}

