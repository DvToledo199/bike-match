package com.bikematch.kinematics;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validates the monopivot engine against real bikes with known reference
 * curves (bikechecker.com), within the ±3% tolerance the plan allows for
 * hand-marked photos.
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
}
