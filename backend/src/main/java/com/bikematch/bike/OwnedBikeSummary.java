package com.bikematch.bike;

import java.time.Instant;

public record OwnedBikeSummary(
        Long id,
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        String photoUrl,
        BikeStatus status,
        boolean analyzed,
        Instant createdAt
) {
}