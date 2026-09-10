package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.ChainDrive;
import com.bikematch.kinematics.geometry.Point2D;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CogAwareKickbackTest {
    private final Point2D bottomBracket = new Point2D(440, 0);
    private final List<Point2D> path = List.of(new Point2D(0, 0), new Point2D(-30, -100));

    @Test
    void equalGearsCancelWrapLeavingGrowthAndRollback() {
        var curve = KickbackCurve.from(path, bottomBracket, new ChainDrive(32, 32), 371);
        double growth = (Math.hypot(470, 100) - 440) / (32 * 12.7 / (2 * Math.PI));
        assertEquals(Math.toDegrees(growth + 30.0 / 371), curve.samples().getLast().kickbackDegrees(), 1e-10);
        assertEquals(new KickbackSample(0, 0), curve.samples().getFirst());
    }

    @Test
    void gearAndRadiusAffectTheResult() {
        double highGear = KickbackCurve.from(path, bottomBracket, new ChainDrive(32, 10), 371)
                .samples().getLast().kickbackDegrees();
        double climbingGear = KickbackCurve.from(path, bottomBracket, new ChainDrive(32, 50), 371)
                .samples().getLast().kickbackDegrees();
        double smallerWheel = KickbackCurve.from(path, bottomBracket, new ChainDrive(32, 50), 352)
                .samples().getLast().kickbackDegrees();
        assertTrue(climbingGear > highGear);
        assertTrue(smallerWheel > climbingGear);
    }

    @Test
    void constantChainDistanceStillIncludesWrapAndRollback() {
        double theta = 0.2;
        // Circle about BB: tangent span length remains constant while its angle changes.
        var circular = List.of(new Point2D(0, 0),
                new Point2D(440 - 440 * Math.cos(theta), -440 * Math.sin(theta)));
        var drive = new ChainDrive(32, 50);
        double expected = theta * (50.0 / 32 - 1)
                - (440 - 440 * Math.cos(theta)) / 371 * (50.0 / 32);
        var curve = KickbackCurve.from(circular, bottomBracket, drive, 371);
        assertEquals(Math.toDegrees(expected), curve.samples().getLast().kickbackDegrees(), 1e-10);
    }

    @Test
    void forwardRotationIsPreservedRatherThanClipped() {
        var advancing = List.of(new Point2D(0, 0), new Point2D(30, -10));
        var curve = KickbackCurve.from(advancing, bottomBracket, new ChainDrive(32, 10), 371);
        assertTrue(curve.samples().getLast().kickbackDegrees() < 0);
    }

    @Test
    void badInputsFailAndSamplesCannotBeMutated() {
        assertThrows(IllegalArgumentException.class, () -> KickbackCurve.from(path, bottomBracket,
                new ChainDrive(32, 50), Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> KickbackCurve.from(path, null,
                new ChainDrive(32, 50), 371));
        var curve = KickbackCurve.from(path, bottomBracket, new ChainDrive(32, 50), 371);
        assertThrows(UnsupportedOperationException.class, () -> curve.samples().clear());
    }
}
