package com.bikematch.moderation.api;

import com.bikematch.moderation.BikeRemovalNotice;
import java.time.Instant;

public record RemovalNoticeResponse(
        Long id,
        String brand,
        String model,
        String reason,
        Instant removedAt
) {

    public static RemovalNoticeResponse from(BikeRemovalNotice notice) {
        return new RemovalNoticeResponse(
                notice.getId(),
                notice.getBrand(),
                notice.getModel(),
                notice.getReason(),
                notice.getRemovedAt()
        );
    }
}
