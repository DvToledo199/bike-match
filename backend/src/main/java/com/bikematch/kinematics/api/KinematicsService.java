package com.bikematch.kinematics.api;

import com.bikematch.kinematics.check.TravelCheck;
import com.bikematch.kinematics.curve.KickbackCurve;
import com.bikematch.kinematics.curve.LeverageCurve;
import com.bikematch.kinematics.curve.SuspensionResponseCurves;
import com.bikematch.kinematics.geometry.ChainDrive;
import com.bikematch.kinematics.descriptor.AxlePathDescriptors;
import com.bikematch.kinematics.descriptor.LeverageDescriptors;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.KinematicsParameters;
import com.bikematch.kinematics.model.MarkedPoint;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.ReferenceSetup;
import com.bikematch.kinematics.solver.MonopivotSolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.EnumSet;

@Service
public class KinematicsService {

    private final MonopivotSolver solver = new MonopivotSolver();

    /** Turns the request (points in pixels + calibration) into the engine's input (points in mm). */
    private PreparedInput prepareInput(PreviewRequest request) {
        EnumSet<PointType> types = EnumSet.noneOf(PointType.class);
        for (PointDto point : request.points()) {
            if (!types.add(point.type())) {
                throw new IllegalArgumentException("Duplicate point: " + point.type());
            }
        }
        if (!types.equals(EnumSet.allOf(PointType.class))) {
            throw new IllegalArgumentException("Mark each of the six required points exactly once");
        }
        Point2D shockFrame = pixelPointOf(request.points(), PointType.SHOCK_FRAME);
        Point2D shockSwingarm = pixelPointOf(request.points(), PointType.SHOCK_SWINGARM);
        double referenceDistance = shockFrame.distanceTo(shockSwingarm);
        if (!Double.isFinite(referenceDistance) || referenceDistance < 0.01) {
            throw new IllegalArgumentException("The two shock mounts must be distinct");
        }
        double mmPerPixel = request.eyeToEyeMm() / referenceDistance;

        Point2D rearAxle = pixelPointOf(request.points(), PointType.REAR_AXLE);
        Point2D frontAxle = pixelPointOf(request.points(), PointType.FRONT_AXLE);
        double axleDx = frontAxle.x() - rearAxle.x();
        double axleDy = frontAxle.y() - rearAxle.y();
        if (Math.abs(axleDx) < 1) {
            throw new IllegalArgumentException("Use a level side photo with clearly separated wheel axles");
        }
        double facing = Math.signum(axleDx);
        double observedAngle = Math.atan2(axleDy, Math.abs(axleDx));
        var wheels = request.parameters().wheelConfiguration();
        double rotation = 0;
        if (wheels != null) {
            double axleDistanceMm = Math.hypot(axleDx, axleDy) * mmPerPixel;
            double radiusDifference = wheels.rearRadiusMm() - wheels.frontRadiusMm();
            if (axleDistanceMm <= Math.abs(radiusDifference)) {
                throw new IllegalArgumentException("Wheel spacing is incompatible with the selected wheels");
            }
            rotation = Math.asin(radiusDifference / axleDistanceMm) - observedAngle;
        }
        if (Math.abs(wheels == null ? observedAngle : rotation) > Math.toRadians(15)) {
            throw new IllegalArgumentException("Use a side photo within 15 degrees of level and check the wheel selection");
        }
        final double correction = rotation;

        List<MarkedPoint> points = request.points().stream()
                .map(dto -> new MarkedPoint(dto.type(),
                        new Point2D(facing * (dto.x() - rearAxle.x()) * mmPerPixel,
                                (dto.y() - rearAxle.y()) * mmPerPixel)
                                .rotateAround(new Point2D(0, 0), correction)))
                .toList();

        KinematicsParametersDto params = request.parameters();
        KinematicsParameters parameters = new KinematicsParameters(
                params.shockStrokeMm(), params.chainringTeeth(), params.sprocketTeeth(),
                params.declaredTravelMm(), params.sagPercent());

        return new PreparedInput(new KinematicsInput(points, parameters), Math.toDegrees(rotation));
    }

    private record PreparedInput(KinematicsInput input, double photoRotationDegrees) { }

    private Point2D pixelPointOf(List<PointDto> points, PointType type) {
        for (PointDto point : points) {
            if (point.type() == type) {
                return new Point2D(point.x(), point.y());
            }
        }
        throw new IllegalArgumentException("Missing point: " + type);
    }

    public PreviewResponse preview(PreviewRequest request) {
        PreparedInput prepared = prepareInput(request);
        KinematicsInput input = prepared.input();
        KinematicsParametersDto parameters = request.parameters();

        List<Point2D> axlePath = solver.sweep(input);

        LeverageCurve leverageCurve = LeverageCurve.from(axlePath, parameters.shockStrokeMm());
        ReferenceSetup setup = parameters.wheelConfiguration() == null ? null
                : ReferenceSetup.standard(parameters.wheelConfiguration());
        KickbackCurve kickbackCurve = setup == null
                ? KickbackCurve.from(axlePath, input.pointOf(PointType.BOTTOM_BRACKET), parameters.chainringTeeth())
                : KickbackCurve.from(axlePath, input.pointOf(PointType.BOTTOM_BRACKET),
                        new ChainDrive(parameters.chainringTeeth(), parameters.sprocketTeeth()), setup.wheels().rearRadiusMm());
        SuspensionResponseCurves responseCurves = setup == null
                ? new SuspensionResponseCurves(List.of(), List.of())
                : SuspensionResponseCurves.from(axlePath, input, setup);

        LeverageDescriptors leverageDescriptors = LeverageDescriptors.from(leverageCurve, parameters.sagPercent());
        AxlePathDescriptors axlePathDescriptors = AxlePathDescriptors.from(axlePath);
        TravelCheck travelCheck = TravelCheck.from(axlePath, parameters.declaredTravelMm());

        MeasurementConditions conditions = new MeasurementConditions(
                parameters.sagPercent(), parameters.chainringTeeth(), parameters.sprocketTeeth(),
                setup == null ? "monopivot-v1" : "monopivot-reference-v2",
                setup == null ? null : ReferenceAssumptions.from(setup, prepared.photoRotationDegrees()));

        return new PreviewResponse(
                leverageCurve.samples(),
                kickbackCurve.samples(),
                axlePath,
                leverageDescriptors,
                axlePathDescriptors,
                travelCheck,
                conditions,
                responseCurves.antiSquat(),
                responseCurves.antiRise());
    }
}
