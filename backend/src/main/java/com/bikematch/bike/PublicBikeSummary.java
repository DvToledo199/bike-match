package com.bikematch.bike;

import java.time.Instant;

public record PublicBikeSummary(
        Long id,
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        String photoUrl,
        Instant createdAt
) {
}
