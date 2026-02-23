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

