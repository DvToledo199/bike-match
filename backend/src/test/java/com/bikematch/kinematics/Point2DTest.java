package com.bikematch.kinematics;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Point2DTest {

    @Test
    void distanceBetweenTwoPoints() {
        Point2D a = new Point2D(0, 0);
        Point2D b = new Point2D(3, 4);
        assertEquals(5.0, a.distanceTo(b));
    }
}