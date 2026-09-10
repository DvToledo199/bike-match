package com.bikematch.kinematics.api;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReferencePreviewTest {
    private final KinematicsService service = new KinematicsService();

    private List<PointDto> points(WheelConfiguration wheels) {
        double scale = 210 / Math.hypot(922.5 - 760.4, 640 - 660.6);
        return List.of(new PointDto(PointType.MAIN_PIVOT, 804.9, 795.8),
                new PointDto(PointType.SHOCK_FRAME, 922.5, 640.0),
                new PointDto(PointType.SHOCK_SWINGARM, 760.4, 660.6),
                new PointDto(PointType.BOTTOM_BRACKET, 778.4, 855.4),
                new PointDto(PointType.REAR_AXLE, 409.0, 826.1),
                new PointDto(PointType.FRONT_AXLE, 1432.5,
                        826.1 + (wheels.rearRadiusMm() - wheels.frontRadiusMm()) / scale));
    }

    private PreviewRequest request(List<PointDto> points, WheelConfiguration wheels, int cog) {
        return new PreviewRequest(points, 210.0, new KinematicsParametersDto(55.0, 32, cog, 150.0, 30.0, wheels));
    }

    @ParameterizedTest
    @EnumSource(WheelConfiguration.class)
    void rotatedAndReflectedPhotosPreserveAllFiveCurves(WheelConfiguration wheels) {
        var original = service.preview(request(points(wheels), wheels, 50));
        for (int facing : new int[] {1, -1}) {
            var rotated = points(wheels).stream().map(p -> {
                var point = new Point2D(p.x(), p.y()).rotateAround(new Point2D(409, 826.1), Math.toRadians(8));
                return new PointDto(p.type(), 2000 + facing * point.x(), point.y() + 300);
            }).toList();
            var result = service.preview(request(rotated, wheels, 50));
            assertEquals(-8, result.conditions().reference().photoRotationDegrees(), 1e-9);
            for (int i = 0; i < original.axlePath().size(); i++) {
                assertEquals(original.axlePath().get(i).x(), result.axlePath().get(i).x(), 1e-7);
                assertEquals(original.axlePath().get(i).y(), result.axlePath().get(i).y(), 1e-7);
                assertEquals(original.kickbackCurve().get(i).kickbackDegrees(), result.kickbackCurve().get(i).kickbackDegrees(), 1e-7);
                assertEquals(original.antiSquatCurve().get(i).percent(), result.antiSquatCurve().get(i).percent(), 1e-7);
                assertEquals(original.antiRiseCurve().get(i).percent(), result.antiRiseCurve().get(i).percent(), 1e-7);
            }
            for (int i = 0; i < original.leverageCurve().size(); i++) {
                assertEquals(original.leverageCurve().get(i).ratio(), result.leverageCurve().get(i).ratio(), 1e-7);
            }
        }
        assertEquals(0, original.conditions().reference().photoRotationDegrees(), 1e-9);
        assertEquals(wheels.frontRadiusMm(), original.conditions().reference().frontWheelRadiusMm());
        assertEquals(wheels.rearRadiusMm(), original.conditions().reference().rearWheelRadiusMm());
        assertEquals(1100, original.conditions().reference().centerOfGravityHeightMm());
        assertEquals("monopivot-reference-v2", original.conditions().modelVersion());
    }

    @Test
    void gearingChangesOnlyDriveRelatedCurves() {
        var points = points(WheelConfiguration.FULL_29);
        var large = service.preview(request(points, WheelConfiguration.FULL_29, 50));
        var small = service.preview(request(points, WheelConfiguration.FULL_29, 10));
        assertNotEquals(large.antiSquatCurve(), small.antiSquatCurve());
        assertNotEquals(large.kickbackCurve(), small.kickbackCurve());
        assertEquals(large.leverageCurve(), small.leverageCurve());
        assertEquals(large.antiRiseCurve(), small.antiRiseCurve());
    }

    @Test
    void excessiveTiltFailsWithoutPublishingAnInventedCorrection() {
        var points = points(WheelConfiguration.MULLET).stream().map(p -> {
            var rotated = new Point2D(p.x(), p.y()).rotateAround(new Point2D(409, 826.1), Math.toRadians(25));
            return new PointDto(p.type(), rotated.x(), rotated.y());
        }).toList();
        assertThrows(IllegalArgumentException.class, () -> service.preview(request(points, WheelConfiguration.MULLET, 50)));
    }

    @Test
    void legacyRequestsDoNotInventWheelOrCgData() {
        var result = service.preview(request(points(WheelConfiguration.FULL_29), null, 50));
        assertEquals("monopivot-v1", result.conditions().modelVersion());
        assertNull(result.conditions().reference());
        assertTrue(result.antiSquatCurve().isEmpty());
        assertTrue(result.antiRiseCurve().isEmpty());
    }
}
