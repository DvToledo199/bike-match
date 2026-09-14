package com.bikematch.moderation.api;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeStatus;

public record ModerationDecisionResponse(Long id, BikeStatus status) {

    public static ModerationDecisionResponse from(Bike bike) {
        return new ModerationDecisionResponse(bike.getId(), bike.getStatus());
    }
}
