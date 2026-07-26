package com.bikematch.kinematics;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonopivotSolverFixtureTest {

    @Test
    void computedTravelMatchesTheOrangeStage6() {
        OrangeStage6Fixture bike = new OrangeStage6Fixture();

        List<Point2D> axlePath = new MonopivotSolver().sweep(bike.input());

        Point2D atRest = axlePath.get(0);
        Point2D fullyCompressed = axlePath.get(axlePath.size() - 1);
        double travelMm = atRest.y() - fullyCompressed.y();   // image y grows downwards

        double declaredMm = bike.declaredTravelMm();
        assertEquals(declaredMm, travelMm, declaredMm * 0.03);
    }
}