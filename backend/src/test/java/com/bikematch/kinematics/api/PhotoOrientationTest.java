package com.bikematch.kinematics.api;

import com.bikematch.kinematics.model.PointType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PhotoOrientationTest {
    private final KinematicsService service = new KinematicsService();
    private final List<PointDto> points = List.of(
            new PointDto(PointType.MAIN_PIVOT, 208.3, 167.8),
            new PointDto(PointType.SHOCK_FRAME, 250.9, 114.4),
            new PointDto(PointType.SHOCK_SWINGARM, 192.8, 121.3),
            new PointDto(PointType.BOTTOM_BRACKET, 198.7, 190.3),
            new PointDto(PointType.REAR_AXLE, 86.0, 187.3),
            new PointDto(PointType.FRONT_AXLE, 408.5, 190.0));

    private PreviewRequest request(List<PointDto> markedPoints) {
        return new PreviewRequest(markedPoints, 230.0,
                new KinematicsParametersDto(65.0, 34, 50, 164.0, 30.0));
    }

    @Test
    void reflectionAndTranslationPreserveEveryCurveSample() {
        PreviewResponse original = service.preview(request(points));
        PreviewResponse reflected = service.preview(request(points.stream()
                .map(p -> new PointDto(p.type(), 1000 - p.x(), p.y() + 200)).toList()));
        for (int i = 0; i < original.axlePath().size(); i++) {
            assertEquals(original.axlePath().get(i).x(), reflected.axlePath().get(i).x(), 1e-9);
            assertEquals(original.axlePath().get(i).y(), reflected.axlePath().get(i).y(), 1e-9);
        }
        for (int i = 0; i < original.leverageCurve().size(); i++) {
            assertEquals(original.leverageCurve().get(i).ratio(), reflected.leverageCurve().get(i).ratio(), 1e-9);
        }
        for (int i = 0; i < original.kickbackCurve().size(); i++) {
            assertEquals(original.kickbackCurve().get(i).kickbackDegrees(), reflected.kickbackCurve().get(i).kickbackDegrees(), 1e-9);
        }
        assertEquals(original.leverageDescriptors().progressionBand(), reflected.leverageDescriptors().progressionBand());
        assertEquals(original.travelCheck().calculatedTravelMm(), reflected.travelCheck().calculatedTravelMm(), 1e-9);
        assertEquals(original.axlePathDescriptors().maxRearwardMm(), reflected.axlePathDescriptors().maxRearwardMm(), 1e-9);
    }

    @Test
    void ambiguousWheelOrientationIsRejected() {
        List<PointDto> invalid = points.stream().map(p -> p.type() == PointType.FRONT_AXLE
                ? new PointDto(p.type(), 86.0, 190.0) : p).toList();
        assertThrows(IllegalArgumentException.class, () -> service.preview(request(invalid)));
    }
}
