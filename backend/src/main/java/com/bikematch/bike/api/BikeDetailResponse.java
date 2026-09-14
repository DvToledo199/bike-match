package com.bikematch.bike.api;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.BikeStatus;
import com.bikematch.bike.CassetteType;
import com.bikematch.bike.GetBikeDetailService.BikeDetail;
import com.bikematch.bike.GetBikeDetailService.KinematicsResultDetail;
import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.WheelConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record BikeDetailResponse(
        Long id,
        String brand,
        String model,
        Short modelYear,
        BikeCategory category,
        SuspensionLayout suspensionLayout,
        double declaredTravelMm,
        double shockEyeToEyeMm,
        double shockStrokeMm,
        WheelConfiguration wheelConfiguration,
        CassetteType cassetteType,
        short chainringTeeth,
        short sprocketTeeth,
        double sagPercent,
        String photoUrl,
        BikeStatus status,
        boolean analyzed,
        String ownerUsername,
        Instant createdAt,
        KinematicsResultResponse result
) {

    public static BikeDetailResponse from(BikeDetail detail) {
        return new BikeDetailResponse(
                detail.id(),
                detail.brand(),
                detail.model(),
                detail.modelYear(),
                detail.category(),
                detail.suspensionLayout(),
                detail.declaredTravelMm(),
                detail.shockEyeToEyeMm(),
                detail.shockStrokeMm(),
                detail.wheelConfiguration(),
                detail.cassetteType(),
                detail.chainringTeeth(),
                detail.sprocketTeeth(),
                detail.sagPercent(),
                detail.photoUrl(),
                detail.status(),
                detail.result() != null,
                detail.ownerUsername(),
                detail.createdAt(),
                detail.result() == null ? null : KinematicsResultResponse.from(detail.result())
        );
    }

    public record KinematicsResultResponse(
            int resultVersion,
            String engineVersion,
            JsonNode curves,
            JsonNode descriptors,
            JsonNode capabilities,
            Instant computedAt
    ) {

        private static KinematicsResultResponse from(KinematicsResultDetail result) {
            return new KinematicsResultResponse(
                    result.resultVersion(),
                    result.engineVersion(),
                    result.curves(),
                    result.descriptors(),
                    result.capabilities(),
                    result.computedAt()
            );
        }
    }
}
