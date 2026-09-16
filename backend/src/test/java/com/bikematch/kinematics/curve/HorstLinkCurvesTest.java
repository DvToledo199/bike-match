package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkCurveInput;
import com.bikematch.kinematics.model.HorstLinkGeometry;
import com.bikematch.kinematics.model.KinematicsParameters;
import com.bikematch.kinematics.model.ReferenceSetup;
import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.kinematics.solver.HorstLinkPosition;
import com.bikematch.kinematics.solver.HorstLinkSolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HorstLinkCurvesTest {
    private final HorstLinkGeometry geometry = new HorstLinkGeometry(
            new Point2D(0, 0),
            new Point2D(-100, 0),
            new Point2D(0, -120),
            new Point2D(-100, -120),
            new Point2D(50, -160),
            new Point2D(-40, -120),
            new Point2D(-160, -90));

    @Test
    void producesTheFiveFiniteCurvesFromOneHorstLinkSweep() {
        List<HorstLinkPosition> positions = new HorstLinkSolver().sweep(geometry, 10);
        HorstLinkCurves curves = HorstLinkCurves.from(positions, curveInput());

        assertEquals(101, curves.axlePath().size());
        assertEquals(100, curves.leverage().samples().size());
        assertEquals(101, curves.kickback().samples().size());
        assertEquals(101, curves.responses().antiSquat().size());
        assertEquals(101, curves.responses().antiRise().size());
        assertAllFinite(curves);
    }

    @Test
    void supportsAParallelInstantCenterAtRestWithoutDividingByZero() {
        HorstLinkCurves curves = HorstLinkCurves.from(
                new HorstLinkSolver().sweep(geometry, 10), curveInput());

        assertEquals(0, curves.responses().antiRise().getFirst().percent(), 1e-9);
    }

    private HorstLinkCurveInput curveInput() {
        return new HorstLinkCurveInput(geometry, new Point2D(250, -30), new Point2D(1_100, 0),
                new KinematicsParameters(10, 32, 52, 100, 30),
                ReferenceSetup.standard(WheelConfiguration.FULL_29));
    }

    private void assertAllFinite(HorstLinkCurves curves) {
        assertTrue(curves.leverage().samples().stream().allMatch(sample -> Double.isFinite(sample.ratio())));
        assertTrue(curves.kickback().samples().stream()
                .allMatch(sample -> Double.isFinite(sample.kickbackDegrees())));
        assertTrue(curves.responses().antiSquat().stream().allMatch(sample -> Double.isFinite(sample.percent())));
        assertTrue(curves.responses().antiRise().stream().allMatch(sample -> Double.isFinite(sample.percent())));
    }
}
