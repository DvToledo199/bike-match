package com.bikematch.bike.api;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.BikeStatus;
import com.bikematch.bike.OwnedBikeSummary;
import java.time.Instant;

public record MyBikeSummaryResponse(
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

    public static MyBikeSummaryResponse from(OwnedBikeSummary summary) {
        return new MyBikeSummaryResponse(
                summary.id(),
                summary.brand(),
                summary.model(),
                summary.modelYear(),
                summary.category(),
                summary.photoUrl(),
                summary.status(),
                summary.analyzed(),
                summary.createdAt()
        );
    }
}
