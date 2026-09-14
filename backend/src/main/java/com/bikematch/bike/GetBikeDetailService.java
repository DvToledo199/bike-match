package com.bikematch.bike;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bikematch.kinematics.model.WheelConfiguration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetBikeDetailService {

    private final BikeRepository bikeRepository;
    private final ObjectMapper objectMapper;

    public GetBikeDetailService(BikeRepository bikeRepository, ObjectMapper objectMapper) {
        this.bikeRepository = bikeRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BikeDetail get(long bikeId, Long viewerId) {
        Bike bike = bikeRepository.findById(bikeId)
                .filter(foundBike -> foundBike.canBeViewedBy(viewerId))
                .orElseThrow(BikeNotFoundException::new);

        return toDetail(bike);
    }

    private BikeDetail toDetail(Bike bike) {
        KinematicsResult result = bike.getKinematicsResult();
        return new BikeDetail(
                bike.getId(),
                bike.getBrand(),
                bike.getModel(),
                bike.getModelYear(),
                bike.getCategory(),
                bike.getSuspensionLayout(),
                bike.getDeclaredTravelMm(),
                bike.getShockEyeToEyeMm(),
                bike.getShockStrokeMm(),
                bike.getWheelConfiguration(),
                bike.getCassetteType(),
                bike.getChainringTeeth(),
                bike.getSprocketTeeth(),
                bike.getSagPercent(),
                bike.getPhotoUrl(),
                bike.getStatus(),
                bike.getOwner().getUsername(),
                bike.getCreatedAt(),
                result == null ? null : toResult(result)
        );
    }

    private KinematicsResultDetail toResult(KinematicsResult result) {
        return new KinematicsResultDetail(
                result.getResultVersion(),
                result.getEngineVersion(),
                parseObject(result.getCurves(), "curves"),
                parseObject(result.getDescriptors(), "descriptors"),
                parseObject(result.getCapabilities(), "capabilities"),
                result.getComputedAt()
        );
    }

    private JsonNode parseObject(String json, String fieldName) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new IllegalStateException("Stored kinematics " + fieldName
                        + " must be a JSON object");
            }
            return node;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored kinematics " + fieldName
                    + " is not valid JSON", exception);
        }
    }

    public record BikeDetail(
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
            String ownerUsername,
            Instant createdAt,
            KinematicsResultDetail result
    ) {
    }

    public record KinematicsResultDetail(
            int resultVersion,
            String engineVersion,
            JsonNode curves,
            JsonNode descriptors,
            JsonNode capabilities,
            Instant computedAt
    ) {
    }
}
