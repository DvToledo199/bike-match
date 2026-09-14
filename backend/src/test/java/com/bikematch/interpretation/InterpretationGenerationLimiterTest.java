package com.bikematch.interpretation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class InterpretationGenerationLimiterTest {

    @Test
    void blocksRepeatedGenerationForTheSameOwnerAndBikeDuringCooldown() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-14T12:00:00Z"), ZoneOffset.UTC);
        InterpretationGenerationLimiter limiter =
                new InterpretationGenerationLimiter(Duration.ofSeconds(30), clock);

        assertThat(limiter.tryAcquire(42L, 7L)).isTrue();
        assertThat(limiter.tryAcquire(42L, 7L)).isFalse();
        assertThat(limiter.tryAcquire(42L, 8L)).isTrue();
        assertThat(limiter.tryAcquire(43L, 7L)).isTrue();
    }
}
