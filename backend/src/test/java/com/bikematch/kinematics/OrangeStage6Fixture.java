package com.bikematch.kinematics;

import java.util.List;

/**
 * Orange Stage 6 29" 2020 — reference bike used to validate the engine.
 * Points marked by hand on a 1800x1200 px side photo; specs from the
 * manufacturer; reference curves from bikechecker.com.
 * Coordinates keep the photo's orientation: y grows downwards.
 */
class OrangeStage6Fixture {

    private final double eyeToEyeMm = 210.0;

    // Points marked on the photo, in pixels
    private final Point2D pivot = new Point2D(806, 797);
    private final Point2D shockFrame = new Point2D(923, 639);
    private final Point2D shockSwingarm = new Point2D(761, 662);
    private final Point2D bottomBracket = new Point2D(778, 852);
    private final Point2D rearAxle = new Point2D(410, 827);
    private final Point2D frontAxle = new Point2D(1431, 816);

    double declaredTravelMm() {
        return 150.0;
    }

    KinematicsInput input() {
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