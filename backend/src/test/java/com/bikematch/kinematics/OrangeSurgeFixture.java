package com.bikematch.kinematics;

import java.util.List;

/**
 * Orange Surge 27.5" 2020 — pure single-pivot reference bike, second engine
 * fixture (a flatter, lower leverage curve than the Stage 6). Points marked by
 * hand on a small 500x280 px side photo; shock 230x65, rear travel ~164 mm
 * (read from the reference graph); reference curves from bikechecker.com
 * (LR ~2.55 -> ~2.50). Coordinates keep the photo's orientation: y grows downwards.
 */
class OrangeSurgeFixture implements ReferenceBike {

    private final double eyeToEyeMm = 230.0;

    private final Point2D pivot = new Point2D(208.3, 167.8);
    private final Point2D shockFrame = new Point2D(250.9, 114.4);
    private final Point2D shockSwingarm = new Point2D(192.8, 121.3);
    private final Point2D bottomBracket = new Point2D(198.7, 190.3);
    private final Point2D rearAxle = new Point2D(86.0, 187.3);
    private final Point2D frontAxle = new Point2D(408.5, 190.0);

    @Override
    public double declaredTravelMm() {
        return 164.0;
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

        KinematicsParameters parameters = new KinematicsParameters(65, 32, 50, 164, 30);
        return new KinematicsInput(points, parameters);
    }

    private Point2D inMillimetres(Point2D pixels, double mmPerPixel) {
        return new Point2D(pixels.x() * mmPerPixel, pixels.y() * mmPerPixel);
    }
}
