package com.example.pulse.dbexporter.scrape.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeartbeatParser {
  private HeartbeatParser() {}

  private static final Pattern CURRENT =
      Pattern.compile("^\\s*HA-Node Info \\(current\\s+([^,]+),\\s*state\\s+([^)]+)\\)\\s*$");

  private static final Pattern NODE =
      Pattern.compile("^\\s*Node\\s+(\\S+)\\s+\\(priority\\s+(\\d+),\\s*state\\s+([^)]+)\\)\\s*$");

  private static final Pattern PROCESS_INFO =
      Pattern.compile("^\\s*HA-Process Info \\(master\\s+\\d+,\\s*state\\s+([^)]+)\\)\\s*$");

  private static final Pattern APPLY_COPY =
      Pattern.compile(
          "^\\s*(Applylogdb|Copylogdb)\\s+([^@\\s]+)@[^\\s]+:.*\\(pid\\s+\\d+,\\s*state\\s+([^)]+)\\)\\s*$");

  private static final Pattern SERVER =
      Pattern.compile(
          "^\\s*Server\\s+(\\S+)\\s+\\(pid\\s+\\d+,\\s*state\\s+([^)]+)\\)\\s*$");

  public static HeartbeatSnapshot parse(String stdout) {
    String currentNode = "";
    String currentState = "";
    List<HeartbeatSnapshot.NodeState> nodes = new ArrayList<>();
    List<HeartbeatSnapshot.ProcessState> processes = new ArrayList<>();
    if (stdout == null || stdout.isBlank()) {
      return new HeartbeatSnapshot(currentNode, currentState, nodes, processes);
    }

    for (String line : stdout.split("\n")) {
      String trimmed = line.trim();
      if (trimmed.isEmpty() || trimmed.startsWith("@")) {
        continue;
      }

      Matcher current = CURRENT.matcher(trimmed);
      if (current.matches()) {
        currentNode = current.group(1).trim();
        currentState = current.group(2).trim();
        continue;
      }

      Matcher node = NODE.matcher(trimmed);
      if (node.matches()) {
        String n = node.group(1);
        int priority = parseInt(node.group(2));
        String state = node.group(3).trim();
        nodes.add(new HeartbeatSnapshot.NodeState(n, state, priority));
        continue;
      }

      Matcher procInfo = PROCESS_INFO.matcher(trimmed);
      if (procInfo.matches()) {
        processes.add(new HeartbeatSnapshot.ProcessState("master", procInfo.group(1).trim(), ""));
        continue;
      }

      Matcher ac = APPLY_COPY.matcher(trimmed);
      if (ac.matches()) {
        String name = ac.group(1).toLowerCase(Locale.ROOT);
        String db = ac.group(2).trim();
        String state = ac.group(3).trim();
        processes.add(new HeartbeatSnapshot.ProcessState(name, state, db));
        continue;
      }

      Matcher server = SERVER.matcher(trimmed);
      if (server.matches()) {
        String db = server.group(1).trim();
        String state = server.group(2).trim();
        processes.add(new HeartbeatSnapshot.ProcessState("server", state, db));
      }
    }

    return new HeartbeatSnapshot(currentNode, currentState, nodes, processes);
  }

  private static int parseInt(String s) {
    try {
      return Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return 0;
    }
  }
}

