package com.bikematch.kinematics.geometry;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CircleIntersectionsTest {

    private static final double DELTA = 1e-9;

    @Test
    void returnsBothKnownEquilateralTrianglePositionsInConsistentOrder() {
        List<Point2D> positions = CircleIntersections.between(
                new Point2D(0, 0), 2,
                new Point2D(2, 0), 2);

        assertEquals(2, positions.size());
        assertPoint(positions.get(0), 1, Math.sqrt(3));
        assertPoint(positions.get(1), 1, -Math.sqrt(3));
    }

    @Test
    void returnsOnePositionForTangentCircles() {
        List<Point2D> positions = CircleIntersections.between(
                new Point2D(0, 0), 2,
                new Point2D(4, 0), 2);

        assertEquals(1, positions.size());
        assertPoint(positions.getFirst(), 2, 0);
    }

    @Test
    void returnsNoPositionForSeparateOrContainedCircles() {
        assertEquals(List.of(), CircleIntersections.between(
                new Point2D(0, 0), 1,
                new Point2D(3, 0), 1));
        assertEquals(List.of(), CircleIntersections.between(
                new Point2D(0, 0), 4,
                new Point2D(1, 0), 1));
    }

    @Test
    void rejectsCoincidentCirclesBecauseTheirAssemblyIsAmbiguous() {
        assertThrows(IllegalArgumentException.class, () -> CircleIntersections.between(
                new Point2D(3, -2), 5,
                new Point2D(3, -2), 5));
    }

    @Test
    void preservesSolutionOrderAfterTranslationAndScaling() {
        Point2D firstCentre = new Point2D(100, 250);
        Point2D secondCentre = new Point2D(-200, 250);
        double scale = 7.5;

        List<Point2D> original = CircleIntersections.between(firstCentre, 250, secondCentre, 250);
        List<Point2D> transformed = CircleIntersections.between(
                translateAndScale(firstCentre, scale), 250 * scale,
                translateAndScale(secondCentre, scale), 250 * scale);

        assertEquals(2, original.size());
        assertEquals(2, transformed.size());
        for (int index = 0; index < original.size(); index++) {
            Point2D expected = translateAndScale(original.get(index), scale);
            assertPoint(transformed.get(index), expected.x(), expected.y());
        }
    }

    @Test
    void preservesTheSetOfSolutionsAfterReflection() {
        Point2D firstCentre = new Point2D(100, 250);
        Point2D secondCentre = new Point2D(-200, 250);
        List<Point2D> original = CircleIntersections.between(
                firstCentre, 250, secondCentre, 250);
        List<Point2D> reflected = CircleIntersections.between(
                reflect(firstCentre), 250,
                reflect(secondCentre), 250);

        assertContainsPoint(reflected, reflect(original.get(0)));
        assertContainsPoint(reflected, reflect(original.get(1)));
    }

    @Test
    void rejectsInvalidCircleDefinitions() {
        assertThrows(IllegalArgumentException.class, () -> CircleIntersections.between(
                new Point2D(Double.NaN, 0), 1, new Point2D(0, 0), 1));
        assertThrows(IllegalArgumentException.class, () -> CircleIntersections.between(
                new Point2D(0, 0), -1, new Point2D(0, 0), 1));
        assertThrows(IllegalArgumentException.class, () -> CircleIntersections.between(
                new Point2D(0, 0), 0, new Point2D(0, 0), 1));
    }

    private Point2D translateAndScale(Point2D point, double scale) {
        return new Point2D(1_000 + scale * point.x(), -500 + scale * point.y());
    }

    private Point2D reflect(Point2D point) {
        return new Point2D(-point.x(), point.y());
    }

    private void assertPoint(Point2D point, double expectedX, double expectedY) {
        assertEquals(expectedX, point.x(), DELTA);
        assertEquals(expectedY, point.y(), DELTA);
    }

    private void assertContainsPoint(List<Point2D> points, Point2D expected) {
        boolean matches = points.stream().anyMatch(point -> Math.abs(point.x() - expected.x()) < DELTA
                && Math.abs(point.y() - expected.y()) < DELTA);
        assertTrue(matches, "Expected point was not present: " + expected);
    }
}
