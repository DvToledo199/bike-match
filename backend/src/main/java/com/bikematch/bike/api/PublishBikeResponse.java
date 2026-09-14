package com.bikematch.bike.api;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeStatus;

public record PublishBikeResponse(Long id, BikeStatus status) {

    public static PublishBikeResponse from(Bike bike) {
        return new PublishBikeResponse(bike.getId(), bike.getStatus());
    }
}
