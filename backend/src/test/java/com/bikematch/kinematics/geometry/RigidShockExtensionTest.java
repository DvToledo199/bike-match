package com.bikematch.kinematics.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RigidShockExtensionTest {
    @Test
    void keepsASignedTransverseOffsetWhenTheShockTurnsAndCompresses() {
        for (double offset : new double[]{-12, 0, 12}) {
            Point2D frame = new Point2D(30, 40);
            var extension = RigidShockExtension.from(frame, new Point2D(240, 40),
                    new Point2D(330, 40 + offset));
            assertEquals(90, extension.axialOffsetMm(), 1e-10);
            assertEquals(offset, extension.lateralOffsetMm(), 1e-10);

            // Construct the expected pose independently: a 155 mm shock pointing vertically.
            Point2D joint = new Point2D(30 - offset, 40 + 155 + 90);
            assertEquals(frame.distanceTo(joint), extension.jointDistanceAt(55), 1e-10);
            Point2D eye = extension.movingEyeAt(frame, joint, 55);
            assertEquals(30, eye.x(), 1e-10);
            assertEquals(195, eye.y(), 1e-10);
        }
    }

    @Test
    void validatesShockTravelAndExtensionDirection() {
        assertThrows(IllegalArgumentException.class, () -> new RigidShockExtension(0, 90, 0));
        assertThrows(IllegalArgumentException.class, () -> new RigidShockExtension(210, -90, 0));
        assertThrows(IllegalArgumentException.class, () -> new RigidShockExtension(210, 90, Double.NaN));
        var extension = new RigidShockExtension(210, 90, 0);
        for (double compression : new double[]{-1, 210, 211, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> extension.jointDistanceAt(compression));
        }
    }
}
