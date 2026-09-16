package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.ChainDrive;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.HorstLinkCurveInput;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.ReferenceSetup;
import com.bikematch.kinematics.solver.HorstLinkPosition;

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

    /**
     * Geometric anti-squat and anti-rise estimates for a Horst-link with its brake on the
     * axle-carrying seatstay coupler.
     */
    public static SuspensionResponseCurves fromHorstLink(List<HorstLinkPosition> positions,
                                                          HorstLinkCurveInput input) {
        if (positions == null || positions.size() < 2 || input == null) {
            throw new IllegalArgumentException("Horst-link response inputs are required");
        }
        List<Point2D> axlePath = positions.stream()
                .map(position -> requirePosition(position).rearAxle())
                .toList();
        CurveChecks.compressionPath(axlePath);
        Point2D restAxle = axlePath.getFirst();
        double radius = input.referenceSetup().wheels().rearRadiusMm();
        ChainDrive drive = input.chainDrive();
        List<PercentageSample> squat = new ArrayList<>(positions.size());
        List<PercentageSample> rise = new ArrayList<>(positions.size());

        for (HorstLinkPosition position : positions) {
            HorstLinkPosition current = requirePosition(position);
            Point2D axle = current.rearAxle();
            CurveChecks.finite(axle.x(), "Rear axle X");
            CurveChecks.finite(axle.y(), "Rear axle Y");
            double wheelbase = input.frontAxle().x() - axle.x();
            CurveChecks.positiveFinite(wheelbase, "Wheelbase");

            InstantCenter instantCenter = InstantCenter.of(input.geometry().mainPivot(), current.horstPivot(),
                    input.geometry().rockerFramePivot(), current.rockerSeatstayPivot());
            InstantCenter.RelativeToAxle relative = instantCenter.relativeTo(axle);
            double chainAngle = drive.at(input.bottomBracket(), axle).angleRadians();
            double factor = 100 * wheelbase / input.referenceSetup().centerOfGravityHeightMm();
            double antiSquat = factor * (radius / drive.sprocketRadiusMm()
                    * (Math.sin(chainAngle) - Math.cos(chainAngle) * relative.verticalPerHorizontal())
                    - relative.verticalPerHorizontal());
            double antiRise = factor * (radius * relative.reciprocalHorizontalDistance()
                    - relative.verticalPerHorizontal());
            CurveChecks.finite(antiSquat, "Anti-squat");
            CurveChecks.finite(antiRise, "Anti-rise");
            double travel = restAxle.y() - axle.y();
            squat.add(new PercentageSample(travel, antiSquat));
            rise.add(new PercentageSample(travel, antiRise));
        }
        return new SuspensionResponseCurves(squat, rise);
    }

    private static HorstLinkPosition requirePosition(HorstLinkPosition position) {
        if (position == null) {
            throw new IllegalArgumentException("A Horst-link position is missing");
        }
        return position;
    }

    /**
     * Homogeneous line intersection. Ratios are calculated before division by the homogeneous
     * coordinate, so parallel linkage bars remain a valid centre at infinity.
     */
    private record InstantCenter(double x, double y, double w) {
        static InstantCenter of(Point2D firstStart, Point2D firstEnd,
                                Point2D secondStart, Point2D secondEnd) {
            double firstA = firstStart.y() - firstEnd.y();
            double firstB = firstEnd.x() - firstStart.x();
            double firstC = firstStart.x() * firstEnd.y() - firstEnd.x() * firstStart.y();
            double secondA = secondStart.y() - secondEnd.y();
            double secondB = secondEnd.x() - secondStart.x();
            double secondC = secondStart.x() * secondEnd.y() - secondEnd.x() * secondStart.y();
            return new InstantCenter(
                    firstB * secondC - firstC * secondB,
                    firstC * secondA - firstA * secondC,
                    firstA * secondB - firstB * secondA);
        }

        RelativeToAxle relativeTo(Point2D axle) {
            double horizontal = x - axle.x() * w;
            double vertical = y - axle.y() * w;
            double scale = Math.max(1, Math.max(Math.abs(x), Math.max(Math.abs(y),
                    Math.max(Math.abs(axle.x() * w), Math.abs(axle.y() * w)))));
            if (!Double.isFinite(horizontal) || !Double.isFinite(vertical)
                    || Math.abs(horizontal) <= 1e-10 * scale) {
                throw new IllegalArgumentException("Instant centre lies vertically above the rear axle");
            }
            double verticalPerHorizontal = vertical / horizontal;
            double reciprocalHorizontalDistance = w / horizontal;
            CurveChecks.finite(verticalPerHorizontal, "Instant centre slope");
            CurveChecks.finite(reciprocalHorizontalDistance, "Instant centre horizontal distance");
            return new RelativeToAxle(verticalPerHorizontal, reciprocalHorizontalDistance);
        }

        private record RelativeToAxle(double verticalPerHorizontal, double reciprocalHorizontalDistance) {
        }
    }
}
