package com.bikematch.bike.api;

import com.bikematch.bike.BikeStatus;

public record CreateBikeResponse(Long id, BikeStatus status) {
}
