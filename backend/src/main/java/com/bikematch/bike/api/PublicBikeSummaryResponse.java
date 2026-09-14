package com.bikematch.bike.api;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.PublicBikeSummary;
import java.time.Instant;

public record PublicBikeSummaryResponse(
        Long id,
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        String photoUrl,
        Instant createdAt
) {

    public static PublicBikeSummaryResponse from(PublicBikeSummary summary) {
        return new PublicBikeSummaryResponse(
                summary.id(),
                summary.brand(),
                summary.model(),
                summary.modelYear(),
                summary.category(),
                summary.photoUrl(),
                summary.createdAt());
    }
}
