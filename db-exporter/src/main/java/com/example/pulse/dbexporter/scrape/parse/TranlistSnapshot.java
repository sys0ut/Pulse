package com.example.pulse.dbexporter.scrape.parse;

public record TranlistSnapshot(
    long sessions,
    long activeTransactions,
    long lockWaitTransactions,
    double longestQuerySeconds,
    double longestTranSeconds) {}

