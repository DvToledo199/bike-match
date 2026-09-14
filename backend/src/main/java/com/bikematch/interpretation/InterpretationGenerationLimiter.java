package com.bikematch.interpretation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Small in-memory guard against repeated provider calls for the same owner and bike.
 * Cached interpretations are returned before this guard is consulted.
 */
@Component
public class InterpretationGenerationLimiter {

    private final Duration cooldown;
    private final Clock clock;
    private final ConcurrentMap<String, Instant> lastGenerationByKey = new ConcurrentHashMap<>();

    @Autowired
    public InterpretationGenerationLimiter(
            @Value("${app.interpretation.generation-cooldown:PT30S}") Duration cooldown) {
        this(cooldown, Clock.systemUTC());
    }

    InterpretationGenerationLimiter(Duration cooldown, Clock clock) {
        if (cooldown.isNegative()) {
            throw new IllegalArgumentException("Interpretation generation cooldown cannot be negative");
        }
        this.cooldown = cooldown;
        this.clock = clock;
    }

    public boolean tryAcquire(long ownerId, long bikeId) {
        Instant now = clock.instant();
        String key = ownerId + ":" + bikeId;
        final boolean[] acquired = {false};
        lastGenerationByKey.compute(key, (ignored, previous) -> {
            if (previous != null && now.isBefore(previous.plus(cooldown))) {
                return previous;
            }
            acquired[0] = true;
            return now;
        });
        return acquired[0];
    }
}
