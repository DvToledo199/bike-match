package com.bikematch.kinematics.geometry;

import com.bikematch.kinematics.curve.CurveChecks;

/** Fixed yoke offsets in the shock's own axes; its rocker joint remains free to pivot. */
public record RigidShockExtension(double restShockLengthMm, double axialOffsetMm, double lateralOffsetMm) {
    public RigidShockExtension {
        CurveChecks.positiveFinite(restShockLengthMm, "Shock eye-to-eye length");
        CurveChecks.positiveFinite(axialOffsetMm, "Yoke extension along the shock axis");
        CurveChecks.finite(lateralOffsetMm, "Yoke extension across the shock axis");
    }

    public static RigidShockExtension from(Point2D frameEye, Point2D movingEye, Point2D rockerJoint) {
        double shockLength = frameEye.distanceTo(movingEye);
        CurveChecks.positiveFinite(shockLength, "Shock eye-to-eye length");
        double ux = (movingEye.x() - frameEye.x()) / shockLength;
        double uy = (movingEye.y() - frameEye.y()) / shockLength;
        double dx = rockerJoint.x() - movingEye.x();
        double dy = rockerJoint.y() - movingEye.y();
        return new RigidShockExtension(shockLength, dx * ux + dy * uy, ux * dy - uy * dx);
    }

    public double jointDistanceAt(double compressionMm) {
        return Math.hypot(shockLengthAt(compressionMm) + axialOffsetMm, lateralOffsetMm);
    }

    public Point2D movingEyeAt(Point2D frameEye, Point2D rockerJoint, double compressionMm) {
        double shockLength = shockLengthAt(compressionMm);
        double jointAngle = Math.atan2(rockerJoint.y() - frameEye.y(), rockerJoint.x() - frameEye.x());
        double offsetAngle = Math.atan2(lateralOffsetMm, shockLength + axialOffsetMm);
        double shockAngle = jointAngle - offsetAngle;
        return new Point2D(frameEye.x() + shockLength * Math.cos(shockAngle),
                frameEye.y() + shockLength * Math.sin(shockAngle));
    }

    private double shockLengthAt(double compressionMm) {
        if (!Double.isFinite(compressionMm) || compressionMm < 0 || compressionMm >= restShockLengthMm) {
            throw new IllegalArgumentException("Shock compression must be non-negative and shorter than eye-to-eye length");
        }
        return restShockLengthMm - compressionMm;
    }
}
