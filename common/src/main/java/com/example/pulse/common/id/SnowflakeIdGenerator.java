package com.example.pulse.common.id;

import java.time.Clock;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 64-bit Snowflake-like ID.
 *
 * Layout:
 * - 41 bits: millis since custom epoch
 * - 10 bits: workerId (0..1023)
 * - 12 bits: sequence (0..4095) per millisecond
 */
public final class SnowflakeIdGenerator {
    private static final long EPOCH_MILLIS = 1700000000000L; // 2023-11-14T22:13:20Z

    private final Clock clock;
    private final int workerId;

    private long lastMillis = -1L;
    private int seq = ThreadLocalRandom.current().nextInt(0, 1024);

    public SnowflakeIdGenerator(int workerId) {
        this(workerId, Clock.systemUTC());
    }

    public SnowflakeIdGenerator(int workerId, Clock clock) {
        if (workerId < 0 || workerId > 1023) {
            throw new IllegalArgumentException("workerId must be between 0 and 1023");
        }
        this.workerId = workerId;
        this.clock = clock;
    }

    public synchronized long nextId() {
        long now = clock.millis();
        if (now < lastMillis) {
            // clock moved backwards; clamp to lastMillis
            now = lastMillis;
        }

        if (now == lastMillis) {
            seq = (seq + 1) & 0xFFF;
            if (seq == 0) {
                // sequence overflow; wait for next millisecond
                do {
                    now = clock.millis();
                } while (now <= lastMillis);
            }
        } else {
            seq = ThreadLocalRandom.current().nextInt(0, 1024);
        }

        lastMillis = now;

        long tsPart = (now - EPOCH_MILLIS) & 0x1FFFFFFFFFFL; // 41 bits
        return (tsPart << 22) | ((long) workerId << 12) | (seq & 0xFFF);
    }
}

