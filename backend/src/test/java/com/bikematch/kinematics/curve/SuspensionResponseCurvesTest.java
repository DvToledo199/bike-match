package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SuspensionResponseCurvesTest {
    private final List<Point2D> path = List.of(new Point2D(0, 0), new Point2D(-5, -50));

    private KinematicsInput input(Point2D pivot, int cog) {
        return new KinematicsInput(List.of(new MarkedPoint(PointType.MAIN_PIVOT, pivot),
                new MarkedPoint(PointType.BOTTOM_BRACKET, new Point2D(500, 0)),
                new MarkedPoint(PointType.FRONT_AXLE, new Point2D(1100, 0))),
                new KinematicsParameters(50, 32, cog, 100, 30));
    }

    @Test
    void parallelChainAndSwingarmHaveFiniteZeroAntiSquat() {
        var curves = SuspensionResponseCurves.from(path, input(new Point2D(500, 0), 32),
                ReferenceSetup.standard(WheelConfiguration.FULL_29));
        assertEquals(0, curves.antiSquat().getFirst().percent(), 1e-10);
        // Contact (0,371) -> pivot (500,0); projected height at x=1100 is 816.2 mm.
        assertEquals(74.2, curves.antiRise().getFirst().percent(), 1e-10);
        assertEquals(0, curves.antiSquat().getFirst().wheelTravelMm());
    }

    @Test
    void antiSquatMatchesIndependentGraphicalConstruction() {
        // Equal cogs: the upper chain is horizontal at y=-r. Intersect it with axle->pivot.
        double r = 32 * 12.7 / (2 * Math.PI);
        double intersectionX = 500 * r / 100;
        double projectedHeight = (371 + r) * 1100 / intersectionX;
        var curves = SuspensionResponseCurves.from(path, input(new Point2D(500, -100), 32),
                ReferenceSetup.standard(WheelConfiguration.FULL_29));
        assertEquals(projectedHeight / 1100 * 100, curves.antiSquat().getFirst().percent(), 1e-10);
        assertEquals(94.2, curves.antiRise().getFirst().percent(), 1e-10);
    }

    @Test
    void referenceHeightScalesBothPercentagesAndGearingOnlyChangesAntiSquat() {
        var data = input(new Point2D(500, -100), 32);
        var base = SuspensionResponseCurves.from(path, data, ReferenceSetup.standard(WheelConfiguration.FULL_29));
        var taller = SuspensionResponseCurves.from(path, data, new ReferenceSetup(WheelConfiguration.FULL_29, 2200));
        var gear = SuspensionResponseCurves.from(path, input(new Point2D(500, -100), 50),
                ReferenceSetup.standard(WheelConfiguration.FULL_29));
        for (int i = 0; i < path.size(); i++) {
            assertEquals(base.antiSquat().get(i).percent() / 2, taller.antiSquat().get(i).percent(), 1e-10);
            assertEquals(base.antiRise().get(i).percent() / 2, taller.antiRise().get(i).percent(), 1e-10);
        }
        assertNotEquals(base.antiSquat(), gear.antiSquat());
        assertEquals(base.antiRise(), gear.antiRise());
        assertThrows(UnsupportedOperationException.class, () -> base.antiRise().clear());
    }

    @Test
    void degenerateGeometryIsRejectedRatherThanClamped() {
        assertThrows(IllegalArgumentException.class, () -> SuspensionResponseCurves.from(path,
                input(new Point2D(0, -100), 32), ReferenceSetup.standard(WheelConfiguration.FULL_29)));
        assertThrows(IllegalArgumentException.class, () -> new ReferenceSetup(WheelConfiguration.MULLET, Double.NaN));
    }

    @Test
    void wheelPresetsKeepMulletRadiiSeparate() {
        assertEquals(371, WheelConfiguration.MULLET.frontRadiusMm());
        assertEquals(352, WheelConfiguration.MULLET.rearRadiusMm());
        assertEquals(371, WheelConfiguration.FULL_29.rearRadiusMm());
        assertEquals(352, WheelConfiguration.FULL_27_5.frontRadiusMm());
    }
}
