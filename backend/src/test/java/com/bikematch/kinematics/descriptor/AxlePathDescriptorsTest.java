package com.bikematch.kinematics.descriptor;

import com.bikematch.kinematics.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AxlePathDescriptorsTest {

    @Test
    void findsTheMaxRearwardExcursionAndWhereItHappens() {
        // y grows downwards, so compression lowers y. Rear is smaller x -> rearward = restX - x.
        List<Point2D> axlePath = List.of(
                new Point2D(500, 200),  // rest
                new Point2D(498, 180),  // travel 20, rearward 2
                new Point2D(495, 140),  // travel 60, rearward 5  <- max
                new Point2D(498, 100),  // travel 100, rearward 2
                new Point2D(501, 60)    // travel 140, rearward -1 (forward)
        );

        AxlePathDescriptors d = AxlePathDescriptors.from(axlePath);

        assertEquals(5.0, d.maxRearwardMm(), 1e-9);
        // total travel = 200 - 60 = 140 ; max at travel 60 -> 60 / 140 * 100
        assertEquals(42.857, d.atTravelPercent(), 0.001);
    }

    @Test
    void reportsZeroWhenThePathNeverGoesRearward() {
        List<Point2D> axlePath = List.of(
                new Point2D(500, 200),
                new Point2D(502, 150),
                new Point2D(505, 100)
        );

        AxlePathDescriptors d = AxlePathDescriptors.from(axlePath);

        assertEquals(0.0, d.maxRearwardMm(), 1e-9);
    }
}
