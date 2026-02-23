package com.example.pulse.dbexporter.scrape;

public record CommandResult(int exitCode, String stdout, String stderr, boolean timedOut) {
  public boolean success() {
    return !timedOut && exitCode == 0;
  }
}

