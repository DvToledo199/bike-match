package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkYokeGeometry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Synthetic, independently constructed mechanisms; not a measured bicycle fixture. */
class ShockMountedYokeRegressionTest {
    private static final double EPSILON = 1e-8;
    private final HorstLinkYokeSolver solver = new HorstLinkYokeSolver();

    @Test
    void completesA55mmStrokeThatCannotWorkWithTheEyeFixedToTheRocker() {
        HorstLinkYokeGeometry geometry = collinearGeometry();
        double impossibleOldMinimum = geometry.shockFrame().distanceTo(geometry.rockerFramePivot())
                - geometry.shockYokeEye().distanceTo(geometry.rockerFramePivot());
        assertTrue(impossibleOldMinimum > 210 - 55);

        var positions = solver.sweep(geometry, 55);
        assertEquals(101, positions.size());
        for (var position : positions) {
            assertEquals(210 - position.shockCompressionMm(),
                    geometry.shockFrame().distanceTo(position.shockYokeEye()), EPSILON);
            assertEquals(geometry.shockYokeEye().distanceTo(geometry.yokeRockerPivot()),
                    position.shockYokeEye().distanceTo(position.yokeRockerPivot()), EPSILON);
            assertEquals(100, position.yokeRockerPivot().distanceTo(geometry.rockerFramePivot()), EPSILON);
            assertEquals(100, position.horstPivot().distanceTo(geometry.mainPivot()), EPSILON);
            assertEquals(350, position.horstPivot().distanceTo(position.rockerSeatstayPivot()), EPSILON);
            // The parallelogram coupler translates: an independent closed-form axle position.
            assertEquals(position.rockerSeatstayPivot().x() - 60, position.rearAxle().x(), EPSILON);
            assertEquals(position.rockerSeatstayPivot().y() + 320, position.rearAxle().y(), EPSILON);
            double sx = position.shockYokeEye().x() - geometry.shockFrame().x();
            double sy = position.shockYokeEye().y() - geometry.shockFrame().y();
            double yx = position.yokeRockerPivot().x() - position.shockYokeEye().x();
            double yy = position.yokeRockerPivot().y() - position.shockYokeEye().y();
            assertEquals(0, sx * yy - sy * yx, EPSILON);
        }
        var end = positions.getLast();
        assertNotEquals(geometry.rockerFramePivot().distanceTo(geometry.shockYokeEye()),
                geometry.rockerFramePivot().distanceTo(end.shockYokeEye()), 1);
        assertTrue(end.rearAxle().y() < geometry.rearAxle().y());
    }

    @Test
    void reproducesAKnownRockerAngleFromTheExtendedShockLength() {
        HorstLinkYokeGeometry geometry = collinearGeometry();
        double angle = 0.5;
        Point2D expectedJoint = new Point2D(-100 * Math.cos(angle), -100 * Math.sin(angle));
        double extension = geometry.shockYokeEye().distanceTo(geometry.yokeRockerPivot());
        double finalShock = geometry.shockFrame().distanceTo(expectedJoint) - extension;

        var end = solver.sweep(geometry, 210 - finalShock).getLast();
        assertEquals(expectedJoint.x(), end.yokeRockerPivot().x(), EPSILON);
        assertEquals(expectedJoint.y(), end.yokeRockerPivot().y(), EPSILON);
    }

    @Test
    void rejectsAPhysicallyUnreachableExtensionRatherThanClampingIt() {
        var geometry = collinearGeometry();
        assertThrows(IllegalArgumentException.class, () -> solver.sweep(geometry, 200));
    }

    private HorstLinkYokeGeometry collinearGeometry() {
        Point2D frameEye = new Point2D(250, -150);
        Point2D joint = new Point2D(-100, 0);
        double fraction = 210 / frameEye.distanceTo(joint);
        Point2D eye = new Point2D(frameEye.x() + (joint.x() - frameEye.x()) * fraction,
                frameEye.y() + (joint.y() - frameEye.y()) * fraction);
        return new HorstLinkYokeGeometry(new Point2D(0, 350), new Point2D(-100, 350),
                new Point2D(0, 0), joint, frameEye, joint, eye, new Point2D(-160, 320));
    }
}
