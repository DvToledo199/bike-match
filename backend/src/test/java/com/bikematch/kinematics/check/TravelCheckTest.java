package com.bikematch.kinematics.check;

import com.bikematch.kinematics.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelCheckTest {

    /** A minimal axle path: rest at y=500, bottom-out at y=500-travel (so vertical travel = travelMm). */
    private List<Point2D> pathWithTravel(double travelMm) {
        return List.of(new Point2D(0, 500), new Point2D(0, 500 - travelMm));
    }

    @Test
    void withinToleranceWhenCalculatedIsCloseToDeclared() {
        // calculated 150, declared 155 -> ~3.2% -> within +-10%
        TravelCheck check = TravelCheck.from(pathWithTravel(150), 155);
        assertTrue(check.withinTolerance());
        assertEquals(150, check.calculatedTravelMm(), 1e-9);
    }

    @Test
    void outOfToleranceWhenTheyDifferTooMuch() {
        // calculated 150, declared 120 -> 25% -> outside +-10%
        TravelCheck check = TravelCheck.from(pathWithTravel(150), 120);
        assertFalse(check.withinTolerance());
        assertEquals(25.0, check.deviationPercent(), 0.001);
    }
}
