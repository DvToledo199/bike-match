package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.KinematicsParameters;
import com.bikematch.kinematics.model.MarkedPoint;
import com.bikematch.kinematics.model.PointType;

import java.util.List;

/**
 * Orange Stage 6 29" 2020 — pure single-pivot reference bike.
 * Points re-marked by hand on a 1800x1200 px side photo (magnifier marker);
 * shock 210x55, manufacturer rear travel 150 mm; reference curves from
 * bikechecker.com (LR ~2.775 -> ~2.675). Coordinates keep the photo's
 * orientation: y grows downwards.
 */
class OrangeStage6Fixture implements ReferenceBike {

    private final double eyeToEyeMm = 210.0;

    // Points marked on the photo, in pixels
    private final Point2D pivot = new Point2D(804.9, 795.8);
    private final Point2D shockFrame = new Point2D(922.5, 640.0);
    private final Point2D shockSwingarm = new Point2D(760.4, 660.6);
    private final Point2D bottomBracket = new Point2D(778.4, 855.4);
    private final Point2D rearAxle = new Point2D(409.0, 826.1);
    private final Point2D frontAxle = new Point2D(1432.5, 815.0);

    @Override
    public double declaredTravelMm() {
        return 150.0;
    }

    @Override
    public KinematicsInput input() {
        double mmPerPixel = eyeToEyeMm / shockFrame.distanceTo(shockSwingarm);

        List<MarkedPoint> points = List.of(
                new MarkedPoint(PointType.MAIN_PIVOT, inMillimetres(pivot, mmPerPixel)),
                new MarkedPoint(PointType.SHOCK_FRAME, inMillimetres(shockFrame, mmPerPixel)),
                new MarkedPoint(PointType.SHOCK_SWINGARM, inMillimetres(shockSwingarm, mmPerPixel)),
                new MarkedPoint(PointType.BOTTOM_BRACKET, inMillimetres(bottomBracket, mmPerPixel)),
                new MarkedPoint(PointType.REAR_AXLE, inMillimetres(rearAxle, mmPerPixel)),
                new MarkedPoint(PointType.FRONT_AXLE, inMillimetres(frontAxle, mmPerPixel))
        );

        KinematicsParameters parameters = new KinematicsParameters(55, 32, 50, 150, 30);
        return new KinematicsInput(points, parameters);
    }

    private Point2D inMillimetres(Point2D pixels, double mmPerPixel) {
        return new Point2D(pixels.x() * mmPerPixel, pixels.y() * mmPerPixel);
    }
}
