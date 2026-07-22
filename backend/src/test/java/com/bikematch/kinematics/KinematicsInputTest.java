package com.bikematch.kinematics;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KinematicsInputTest {

    @Test
    void pointOfReturnsThePositionOfTheRequestedType() {
        Point2D pivotPosition = new Point2D(10, 20);
        List<MarkedPoint> points = List.of(
                new MarkedPoint(PointType.MAIN_PIVOT, pivotPosition),
                new MarkedPoint(PointType.REAR_AXLE, new Point2D(100, 30))
        );
        KinematicsInput input = new KinematicsInput(points, anyParameters());

        assertEquals(pivotPosition, input.pointOf(PointType.MAIN_PIVOT));
    }

    @Test
    void pointOfThrowsWhenTheTypeIsMissing() {
        List<MarkedPoint> points = List.of(
                new MarkedPoint(PointType.MAIN_PIVOT, new Point2D(10, 20))
        );
        KinematicsInput input = new KinematicsInput(points, anyParameters());

        assertThrows(IllegalArgumentException.class,
                () -> input.pointOf(PointType.REAR_AXLE));
    }

    private KinematicsParameters anyParameters() {
        return new KinematicsParameters(65, 32, 51, 160, 30);
    }
}