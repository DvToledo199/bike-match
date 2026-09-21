package com.bikematch.kinematics.api;

import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.check.TravelCheck;
import com.bikematch.kinematics.curve.HorstLinkCurves;
import com.bikematch.kinematics.curve.KickbackCurve;
import com.bikematch.kinematics.curve.LeverageCurve;
import com.bikematch.kinematics.curve.SuspensionResponseCurves;
import com.bikematch.kinematics.geometry.ChainDrive;
import com.bikematch.kinematics.descriptor.AxlePathDescriptors;
import com.bikematch.kinematics.descriptor.LeverageDescriptors;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.KinematicsParameters;
import com.bikematch.kinematics.model.HorstLinkCurveInput;
import com.bikematch.kinematics.model.HorstLinkGeometry;
import com.bikematch.kinematics.model.HorstLinkYokeGeometry;
import com.bikematch.kinematics.model.MarkedPoint;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.ReferenceSetup;
import com.bikematch.kinematics.solver.HorstLinkPosition;
import com.bikematch.kinematics.solver.HorstLinkSolver;
import com.bikematch.kinematics.solver.HorstLinkYokePosition;
import com.bikematch.kinematics.solver.HorstLinkYokeSolver;
import com.bikematch.kinematics.solver.MonopivotSolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.EnumSet;

@Service
public class KinematicsService {

    private final MonopivotSolver monopivotSolver = new MonopivotSolver();
    private final HorstLinkSolver horstLinkSolver = new HorstLinkSolver();
    private final HorstLinkYokeSolver horstLinkYokeSolver = new HorstLinkYokeSolver();

    /** Turns the request (points in pixels + calibration) into the engine's input (points in mm). */
    private PreparedInput prepareInput(PreviewRequest request) {
        SuspensionLayout layout = request.suspensionLayout();
        EnumSet<PointType> types = EnumSet.noneOf(PointType.class);
        for (PointDto point : request.points()) {
            if (!types.add(point.type())) {
                throw new IllegalArgumentException("Duplicate point: " + point.type());
            }
        }
        if (!types.equals(layout.requiredPointTypes())) {
            throw new IllegalArgumentException("Mark each of the " + layout.requiredPointTypes().size()
                    + " required points exactly once");
        }
        Point2D shockFrame = pixelPointOf(request.points(), PointType.SHOCK_FRAME);
        Point2D movingShockEye = pixelPointOf(request.points(), layout.movingShockEye());
        double referenceDistance = shockFrame.distanceTo(movingShockEye);
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

        return new PreparedInput(new KinematicsInput(points, parameters), Math.toDegrees(rotation), layout);
    }

    private record PreparedInput(KinematicsInput input, double photoRotationDegrees, SuspensionLayout layout) { }

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
        ReferenceSetup setup = parameters.wheelConfiguration() == null ? null
                : ReferenceSetup.standard(parameters.wheelConfiguration());

        CalculatedCurves calculated = switch (prepared.layout()) {
            case SINGLE_PIVOT -> calculateMonopivot(input, parameters, setup);
            case HORST_LINK -> calculateHorstLink(input, parameters, setup);
            case HORST_LINK_YOKE -> calculateHorstLinkYoke(input, parameters, setup);
        };

        LeverageDescriptors leverageDescriptors = LeverageDescriptors.from(
                calculated.leverageCurve(), parameters.sagPercent());
        AxlePathDescriptors axlePathDescriptors = AxlePathDescriptors.from(calculated.axlePath());
        TravelCheck travelCheck = TravelCheck.from(calculated.axlePath(), parameters.declaredTravelMm());

        MeasurementConditions conditions = new MeasurementConditions(
                parameters.sagPercent(), parameters.chainringTeeth(), parameters.sprocketTeeth(),
                calculated.engineVersion(),
                setup == null ? null : ReferenceAssumptions.from(
                        setup, prepared.photoRotationDegrees(), prepared.layout()));

        return new PreviewResponse(
                calculated.leverageCurve().samples(),
                calculated.kickbackCurve().samples(),
                calculated.axlePath(),
                leverageDescriptors,
                axlePathDescriptors,
                travelCheck,
                conditions,
                calculated.responseCurves().antiSquat(),
                calculated.responseCurves().antiRise());
    }

    private CalculatedCurves calculateMonopivot(KinematicsInput input,
                                                KinematicsParametersDto parameters,
                                                ReferenceSetup setup) {
        List<Point2D> axlePath = monopivotSolver.sweep(input);
        LeverageCurve leverageCurve = LeverageCurve.from(axlePath, parameters.shockStrokeMm());
        KickbackCurve kickbackCurve = setup == null
                ? KickbackCurve.from(axlePath, input.pointOf(PointType.BOTTOM_BRACKET), parameters.chainringTeeth())
                : KickbackCurve.from(axlePath, input.pointOf(PointType.BOTTOM_BRACKET),
                        new ChainDrive(parameters.chainringTeeth(), parameters.sprocketTeeth()),
                        setup.wheels().rearRadiusMm());
        SuspensionResponseCurves responseCurves = setup == null
                ? new SuspensionResponseCurves(List.of(), List.of())
                : SuspensionResponseCurves.from(axlePath, input, setup);
        return new CalculatedCurves(axlePath, leverageCurve, kickbackCurve, responseCurves,
                setup == null ? "monopivot-v1" : "monopivot-reference-v2");
    }

    private CalculatedCurves calculateHorstLink(KinematicsInput input,
                                                KinematicsParametersDto parameters,
                                                ReferenceSetup setup) {
        HorstLinkGeometry geometry = new HorstLinkGeometry(
                input.pointOf(PointType.MAIN_PIVOT),
                input.pointOf(PointType.HORST_PIVOT),
                input.pointOf(PointType.ROCKER_FRAME_PIVOT),
                input.pointOf(PointType.ROCKER_SEATSTAY_PIVOT),
                input.pointOf(PointType.SHOCK_FRAME),
                input.pointOf(PointType.SHOCK_ROCKER),
                input.pointOf(PointType.REAR_AXLE));
        List<HorstLinkPosition> positions = horstLinkSolver.sweep(geometry, parameters.shockStrokeMm());
        List<Point2D> axlePath = positions.stream().map(HorstLinkPosition::rearAxle).toList();

        if (setup == null) {
            LeverageCurve leverageCurve = LeverageCurve.from(axlePath, parameters.shockStrokeMm());
            KickbackCurve kickbackCurve = KickbackCurve.from(
                    axlePath, input.pointOf(PointType.BOTTOM_BRACKET), parameters.chainringTeeth());
            return new CalculatedCurves(axlePath, leverageCurve, kickbackCurve,
                    new SuspensionResponseCurves(List.of(), List.of()), "horst-link-v1");
        }

        HorstLinkCurves curves = HorstLinkCurves.from(positions, new HorstLinkCurveInput(
                geometry, input.pointOf(PointType.BOTTOM_BRACKET), input.pointOf(PointType.FRONT_AXLE),
                input.parameters(), setup));
        return new CalculatedCurves(curves.axlePath(), curves.leverage(), curves.kickback(), curves.responses(),
                "horst-link-reference-v1");
    }

    private CalculatedCurves calculateHorstLinkYoke(KinematicsInput input,
                                                    KinematicsParametersDto parameters,
                                                    ReferenceSetup setup) {
        HorstLinkYokeGeometry geometry = new HorstLinkYokeGeometry(
                input.pointOf(PointType.MAIN_PIVOT),
                input.pointOf(PointType.HORST_PIVOT),
                input.pointOf(PointType.ROCKER_FRAME_PIVOT),
                input.pointOf(PointType.ROCKER_SEATSTAY_PIVOT),
                input.pointOf(PointType.SHOCK_FRAME),
                input.pointOf(PointType.YOKE_ROCKER_PIVOT),
                input.pointOf(PointType.SHOCK_YOKE_EYE),
                input.pointOf(PointType.REAR_AXLE));
        List<HorstLinkYokePosition> yokePositions = horstLinkYokeSolver.sweep(
                geometry, parameters.shockStrokeMm());
        List<HorstLinkPosition> positions = yokePositions.stream()
                .map(HorstLinkYokePosition::asHorstLinkPosition)
                .toList();
        List<Point2D> axlePath = positions.stream().map(HorstLinkPosition::rearAxle).toList();
        HorstLinkGeometry curveGeometry = geometry.asHorstLinkGeometry();

        if (setup == null) {
            LeverageCurve leverageCurve = LeverageCurve.from(axlePath, parameters.shockStrokeMm());
            KickbackCurve kickbackCurve = KickbackCurve.from(
                    axlePath, input.pointOf(PointType.BOTTOM_BRACKET), parameters.chainringTeeth());
            return new CalculatedCurves(axlePath, leverageCurve, kickbackCurve,
                    new SuspensionResponseCurves(List.of(), List.of()), "horst-link-yoke-v2");
        }

        HorstLinkCurves curves = HorstLinkCurves.from(positions, new HorstLinkCurveInput(
                curveGeometry, input.pointOf(PointType.BOTTOM_BRACKET), input.pointOf(PointType.FRONT_AXLE),
                input.parameters(), setup));
        return new CalculatedCurves(curves.axlePath(), curves.leverage(), curves.kickback(), curves.responses(),
                "horst-link-yoke-reference-v2");
    }

    private record CalculatedCurves(
            List<Point2D> axlePath,
            LeverageCurve leverageCurve,
            KickbackCurve kickbackCurve,
            SuspensionResponseCurves responseCurves,
            String engineVersion
    ) {
    }
}
