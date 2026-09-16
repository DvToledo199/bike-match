package com.bikematch.bike;

import com.bikematch.kinematics.api.KinematicsParametersDto;
import com.bikematch.kinematics.api.KinematicsService;
import com.bikematch.kinematics.api.MeasurementConditions;
import com.bikematch.kinematics.api.PointDto;
import com.bikematch.kinematics.api.PreviewRequest;
import com.bikematch.kinematics.api.PreviewResponse;
import com.bikematch.kinematics.check.TravelCheck;
import com.bikematch.kinematics.curve.KickbackSample;
import com.bikematch.kinematics.curve.LeverageSample;
import com.bikematch.kinematics.curve.PercentageSample;
import com.bikematch.kinematics.descriptor.AxlePathDescriptors;
import com.bikematch.kinematics.descriptor.LeverageDescriptors;
import com.bikematch.kinematics.geometry.Point2D;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinalizeBikeAnalysisService {

    private static final int RESULT_VERSION = 1;

    private final BikeRepository bikeRepository;
    private final KinematicsResultRepository resultRepository;
    private final KinematicsService kinematicsService;
    private final ObjectMapper objectMapper;

    public FinalizeBikeAnalysisService(
            BikeRepository bikeRepository,
            KinematicsResultRepository resultRepository,
            KinematicsService kinematicsService,
            ObjectMapper objectMapper
    ) {
        this.bikeRepository = bikeRepository;
        this.resultRepository = resultRepository;
        this.kinematicsService = kinematicsService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PreviewResponse finalizeAnalysis(
            long ownerId,
            long bikeId,
            MarkedPhotoGeometry geometry
    ) {
        Objects.requireNonNull(geometry, "Marked photo geometry is required");
        Bike bike = bikeRepository.findOwnedByIdForUpdate(bikeId, ownerId)
                .orElseThrow(BikeNotFoundException::new);
        bike.requireReadyForAnalysis();

        PreviewResponse response = kinematicsService.preview(toPreviewRequest(bike, geometry));
        StoredAnalysis storedAnalysis = serialize(response);

        bike.attachLinkagePoints(geometry);
        KinematicsResult result = new KinematicsResult(
                bike,
                RESULT_VERSION,
                response.conditions().modelVersion(),
                storedAnalysis.curves(),
                storedAnalysis.descriptors(),
                storedAnalysis.capabilities());

        bikeRepository.save(bike);
        resultRepository.saveAndFlush(result);
        return response;
    }

    private PreviewRequest toPreviewRequest(Bike bike, MarkedPhotoGeometry geometry) {
        if (geometry.suspensionLayout() != bike.getSuspensionLayout()) {
            throw new IllegalArgumentException(
                    "Marked points do not match this bike's suspension layout");
        }
        List<PointDto> points = geometry.points().stream()
                .map(point -> new PointDto(point.type(), point.x(), point.y()))
                .toList();
        KinematicsParametersDto parameters = new KinematicsParametersDto(
                bike.getShockStrokeMm(),
                (int) bike.getChainringTeeth(),
                (int) bike.getSprocketTeeth(),
                bike.getDeclaredTravelMm(),
                bike.getSagPercent(),
                bike.getWheelConfiguration());
        return new PreviewRequest(points, bike.getShockEyeToEyeMm(), parameters,
                bike.getSuspensionLayout());
    }

    private StoredAnalysis serialize(PreviewResponse response) {
        boolean referenceModel = response.conditions().reference() != null;
        StoredCurves curves = new StoredCurves(
                response.leverageCurve(), response.kickbackCurve(), response.axlePath(),
                response.antiSquatCurve(), response.antiRiseCurve());
        StoredDescriptors descriptors = new StoredDescriptors(
                response.leverageDescriptors(), response.axlePathDescriptors(),
                response.travelCheck(), response.conditions());
        StoredCapabilities capabilities = new StoredCapabilities(
                referenceModel,
                !response.antiSquatCurve().isEmpty(),
                !response.antiRiseCurve().isEmpty(),
                referenceModel);
        return new StoredAnalysis(
                toJson(curves), toJson(descriptors), toJson(capabilities));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Analysis result could not be serialized", exception);
        }
    }

    private record StoredAnalysis(String curves, String descriptors, String capabilities) {
    }

    private record StoredCurves(
            List<LeverageSample> leverageCurve,
            List<KickbackSample> kickbackCurve,
            List<Point2D> axlePath,
            List<PercentageSample> antiSquatCurve,
            List<PercentageSample> antiRiseCurve
    ) {
    }

    private record StoredDescriptors(
            LeverageDescriptors leverageDescriptors,
            AxlePathDescriptors axlePathDescriptors,
            TravelCheck travelCheck,
            MeasurementConditions conditions
    ) {
    }

    private record StoredCapabilities(
            boolean cogAwareKickback,
            boolean antiSquat,
            boolean antiRise,
            boolean referenceOnly
    ) {
    }
}
