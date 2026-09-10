package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.ChainDrive;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.ReferenceSetup;

import java.util.ArrayList;
import java.util.List;

/** Geometric estimates for a direct-drive single pivot with its brake fixed to the swingarm. */
public record SuspensionResponseCurves(List<PercentageSample> antiSquat, List<PercentageSample> antiRise) {
    public SuspensionResponseCurves {
        antiSquat = List.copyOf(antiSquat);
        antiRise = List.copyOf(antiRise);
    }

    public static SuspensionResponseCurves from(List<Point2D> axlePath, KinematicsInput input,
                                                 ReferenceSetup setup) {
        CurveChecks.compressionPath(axlePath);
        if (input == null || setup == null) throw new IllegalArgumentException("Analysis setup is required");
        Point2D pivot = input.pointOf(PointType.MAIN_PIVOT);
        Point2D frontAxle = input.pointOf(PointType.FRONT_AXLE);
        Point2D bottomBracket = input.pointOf(PointType.BOTTOM_BRACKET);
        ChainDrive drive = new ChainDrive(input.parameters().chainringTeeth(), input.parameters().sprocketTeeth());
        double radius = setup.wheels().rearRadiusMm();
        double restY = axlePath.getFirst().y();
        List<PercentageSample> squat = new ArrayList<>();
        List<PercentageSample> rise = new ArrayList<>();
        for (Point2D axle : axlePath) {
            double sx = pivot.x() - axle.x();
            double sy = pivot.y() - axle.y();
            double wheelbase = frontAxle.x() - axle.x();
            if (!Double.isFinite(sx) || sx < 0.001) {
                throw new IllegalArgumentException("Main pivot must remain ahead of the rear axle");
            }
            CurveChecks.positiveFinite(wheelbase, "Wheelbase");
            double angle = drive.at(bottomBracket, axle).angleRadians();
            double projection = Math.sin(angle) * sx - Math.cos(angle) * sy;
            double factor = 100 * wheelbase / setup.centerOfGravityHeightMm();
            // Eliminating the chain/swingarm intersection avoids a false singularity when parallel.
            double antiSquat = factor * (radius * projection / drive.sprocketRadiusMm() - sy) / sx;
            double antiRise = factor * (radius - sy) / sx;
            CurveChecks.finite(antiSquat, "Anti-squat");
            CurveChecks.finite(antiRise, "Anti-rise");
            double travel = restY - axle.y();
            squat.add(new PercentageSample(travel, antiSquat));
            rise.add(new PercentageSample(travel, antiRise));
        }
        return new SuspensionResponseCurves(squat, rise);
    }
}
