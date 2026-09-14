package com.bikematch.moderation.api;

import com.bikematch.bike.BikeCategory;
import com.bikematch.moderation.PendingBikeSummary;
import java.time.Instant;

public record PendingBikeResponse(
        Long id,
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        String photoUrl,
        String ownerUsername,
        Instant requestedAt
) {

    public static PendingBikeResponse from(PendingBikeSummary summary) {
        return new PendingBikeResponse(
                summary.id(),
                summary.brand(),
                summary.model(),
                summary.modelYear(),
                summary.category(),
                summary.photoUrl(),
                summary.ownerUsername(),
                summary.requestedAt()
        );
    }
}
