package com.bikematch.moderation;

import com.bikematch.bike.BikeCategory;
import java.time.Instant;

public record PendingBikeSummary(
        Long id,
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        String photoUrl,
        String ownerUsername,
        Instant requestedAt
) {
}
