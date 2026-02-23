package com.example.pulse.dbexporter.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores gauges by (name,tags). Values are updated by the scraper.
 *
 * <p>We use a double encoded into a long to avoid adding extra dependencies.
 */
public final class GaugeStore {
  private final MeterRegistry registry;
  private final Map<Key, AtomicLong> values = new ConcurrentHashMap<>();

  public GaugeStore(MeterRegistry registry) {
    this.registry = Objects.requireNonNull(registry);
  }

  public void set(String name, Tags tags, double value) {
    AtomicLong atomic =
        values.computeIfAbsent(
            new Key(name, tags), k -> registerGauge(name, tags, new AtomicLong()));
    atomic.set(Double.doubleToRawLongBits(value));
  }

  public void set(String name, Tags tags, long value) {
    set(name, tags, (double) value);
  }

  private AtomicLong registerGauge(String name, Tags tags, AtomicLong atomic) {
    Gauge.builder(name, atomic, a -> Double.longBitsToDouble(a.get()))
        .tags(tags)
        .register(registry);
    return atomic;
  }

  private record Key(String name, Tags tags) {
    Key {
      Objects.requireNonNull(name);
      Objects.requireNonNull(tags);
    }
  }
}

