package com.example.pulse.dbexporter.scrape;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ProcessRunner {

  public CommandResult run(List<String> command, Duration timeout)
      throws IOException, InterruptedException {
    Objects.requireNonNull(command);
    Objects.requireNonNull(timeout);

    Process process = new ProcessBuilder(command).redirectErrorStream(false).start();

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    Thread outThread = streamTo(process.getInputStream(), stdout);
    Thread errThread = streamTo(process.getErrorStream(), stderr);

    boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
    if (!finished) {
      terminateProcessTree(process, 500, 1000);
    }

    outThread.join(1000);
    errThread.join(1000);

    int exitCode = finished ? process.exitValue() : -1;
    return new CommandResult(
        exitCode,
        stdout.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8),
        !finished);
  }

  /**
   * Starts the process, captures stdout/stderr for a fixed duration, then forcibly terminates it and
   * returns what was captured.
   *
   * <p>Useful for commands that stream repeated snapshots (e.g. {@code cubrid broker status -f -s 1})
   * where we want to ignore the first snapshot and use a later one.
   */
  public CommandResult runForCapture(List<String> command, Duration captureDuration)
      throws IOException, InterruptedException {
    Objects.requireNonNull(command);
    Objects.requireNonNull(captureDuration);

    Process process = new ProcessBuilder(command).redirectErrorStream(false).start();

    ByteArrayOutputStream stdout = new ByteArrayOutputStream();
    ByteArrayOutputStream stderr = new ByteArrayOutputStream();

    Thread outThread = streamTo(process.getInputStream(), stdout);
    Thread errThread = streamTo(process.getErrorStream(), stderr);

    Thread.sleep(Math.max(0, captureDuration.toMillis()));

    // IMPORTANT:
    // When the command is wrapped (e.g. `script -q -c ... /dev/null`), destroying only the parent
    // can leave the actual child command running as an orphan. Terminate the whole process tree.
    boolean exited = terminateProcessTree(process, 1500, 1000);

    outThread.join(1000);
    errThread.join(1000);

    return new CommandResult(
        exited ? process.exitValue() : -1,
        stdout.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8),
        !exited);
  }

  private static boolean terminateProcessTree(Process process, long gracefulWaitMs, long forceWaitMs)
      throws InterruptedException {
    ProcessHandle root = process.toHandle();

    // Terminate descendants first, then the root, to avoid leaving orphans.
    List<ProcessHandle> toKill = new ArrayList<>();
    root.descendants().forEach(toKill::add);
    toKill.add(root);

    for (ProcessHandle ph : toKill) {
      try {
        ph.destroy();
      } catch (Exception ignored) {
        // best effort
      }
    }

    boolean exited = process.waitFor(gracefulWaitMs, TimeUnit.MILLISECONDS);
    if (exited) {
      return true;
    }

    for (ProcessHandle ph : toKill) {
      try {
        if (ph.isAlive()) {
          ph.destroyForcibly();
        }
      } catch (Exception ignored) {
        // best effort
      }
    }

    process.waitFor(forceWaitMs, TimeUnit.MILLISECONDS);
    return !process.isAlive();
  }

  private static Thread streamTo(InputStream in, ByteArrayOutputStream out) {
    Thread t =
        Thread.ofVirtual()
            .name("process-stream")
            .start(
                () -> {
                  try (in; out) {
                    in.transferTo(out);
                  } catch (IOException ignored) {
                    // Best effort. Scrape will be marked failed by exit code/timeout.
                  }
                });
    return t;
  }
}

