package com.example.pulse.dbexporter.scrape.parse;

import java.util.List;

public record BrokerStatusFs1Snapshot(List<BrokerRow> brokers) {
  public record BrokerRow(String broker, int port, double tps, double qps) {}
}

