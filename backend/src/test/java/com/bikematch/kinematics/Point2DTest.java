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
    @Test
    void rotateAroundSpinsThePointAroundACenter() {
        Point2D point = new Point2D(100, 0);
        Point2D center = new Point2D(0, 0);

        Point2D rotated = point.rotateAround(center, Math.toRadians(90));

        assertEquals(0, rotated.x(), 0.0001);
        assertEquals(100, rotated.y(), 0.0001);
    }
}