package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.curve.KickbackCurve;
import com.bikematch.kinematics.curve.KickbackSample;
import com.bikematch.kinematics.curve.LeverageCurve;
import com.bikematch.kinematics.curve.LeverageSample;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.PointType;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the monopivot engine against real bikes with known reference
 * curves (bikechecker.com). Travel and leverage use the ±3% tolerance the plan
 * allows for hand-marked photos; pedal kickback uses a wider band, as it is far
 * more sensitive to the main-pivot marking (see issue #31).
 */
class MonopivotSolverFixtureTest {

    @Test
    void stage6TravelMatchesTheReference() {
        assertTravelWithin3Percent(new OrangeStage6Fixture());
    }

    @Test
    void stage6LeverageMatchesTheReference() {
        assertLeverageWithin3Percent(new OrangeStage6Fixture(), 2.775, 2.675);
    }

    @Test
    void surgeTravelMatchesTheReference() {
        assertTravelWithin3Percent(new OrangeSurgeFixture());
    }

    @Test
    void surgeLeverageMatchesTheReference() {
        assertLeverageWithin3Percent(new OrangeSurgeFixture(), 2.55, 2.50);
    }

    @Test
    void surgeKickbackMatchesTheReference() {
        // bikechecker.com pedal-kickback graph, Orange Surge at 34/50: ~34 deg at full travel.
        assertPeakKickbackNear(new OrangeSurgeFixture(), 34.0);
    }

    private void assertTravelWithin3Percent(ReferenceBike bike) {
        List<Point2D> axlePath = new MonopivotSolver().sweep(bike.input());
        double travelMm = axlePath.get(0).y() - axlePath.get(axlePath.size() - 1).y();
        double declaredMm = bike.declaredTravelMm();
        assertEquals(declaredMm, travelMm, declaredMm * 0.03);
    }

    private void assertLeverageWithin3Percent(ReferenceBike bike, double expectedInitial, double expectedFinal) {
        KinematicsInput input = bike.input();
        List<Point2D> axlePath = new MonopivotSolver().sweep(input);
        LeverageCurve curve = LeverageCurve.from(axlePath, input.parameters().shockStrokeMm());

        List<LeverageSample> samples = curve.samples();
        double initial = samples.get(0).ratio();
        double last = samples.get(samples.size() - 1).ratio();
        assertEquals(expectedInitial, initial, expectedInitial * 0.03);
        assertEquals(expectedFinal, last, expectedFinal * 0.03);
    }

    private void assertPeakKickbackNear(ReferenceBike bike, double referencePeakDegrees) {
        KinematicsInput input = bike.input();
        List<Point2D> axlePath = new MonopivotSolver().sweep(input);
        KickbackCurve curve = KickbackCurve.from(
                axlePath,
                input.pointOf(PointType.BOTTOM_BRACKET),
                input.parameters().chainringTeeth());

        List<KickbackSample> samples = curve.samples();

        // Kickback grows as the suspension compresses.
        for (int i = 1; i < samples.size(); i++) {
            assertTrue(samples.get(i).kickbackDegrees() >= samples.get(i - 1).kickbackDegrees(),
                    "kickback should not decrease along the travel");
        }

        // Wide +-30% band (not the +-3% used for leverage): pedal kickback is very sensitive
        // to the main-pivot marking, so a hand-marked photo cannot pin it tightly. See #31.
        double peak = samples.get(samples.size() - 1).kickbackDegrees();
        assertEquals(referencePeakDegrees, peak, referencePeakDegrees * 0.30);
    }
}
