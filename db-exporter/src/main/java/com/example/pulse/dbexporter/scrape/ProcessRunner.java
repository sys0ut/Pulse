package com.example.pulse.dbexporter.scrape;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
      process.destroyForcibly();
      process.waitFor(1, TimeUnit.SECONDS);
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

    // Try graceful termination first to allow buffered stdout to flush.
    process.destroy();
    boolean exited = process.waitFor(1500, TimeUnit.MILLISECONDS);
    if (!exited) {
      process.destroyForcibly();
      process.waitFor(1, TimeUnit.SECONDS);
    }

    outThread.join(1000);
    errThread.join(1000);

    return new CommandResult(
        exited ? process.exitValue() : -1,
        stdout.toString(StandardCharsets.UTF_8),
        stderr.toString(StandardCharsets.UTF_8),
        !exited);
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

