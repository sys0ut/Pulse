package com.example.pulse.dbexporter.scrape.parse;

import java.util.List;

public record HeartbeatSnapshot(String currentNode, String currentState, List<NodeState> nodes,
                                List<ProcessState> processes) {
  public record NodeState(String node, String state, int priority) {}

  public record ProcessState(String name, String state, String db) {}
}

