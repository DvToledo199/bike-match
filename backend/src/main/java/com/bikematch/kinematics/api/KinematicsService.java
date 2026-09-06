package com.bikematch.kinematics.api;

import com.bikematch.kinematics.check.TravelCheck;
import com.bikematch.kinematics.curve.KickbackCurve;
import com.bikematch.kinematics.curve.LeverageCurve;
import com.bikematch.kinematics.descriptor.AxlePathDescriptors;
import com.bikematch.kinematics.descriptor.LeverageDescriptors;
import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.KinematicsInput;
import com.bikematch.kinematics.model.KinematicsParameters;
import com.bikematch.kinematics.model.MarkedPoint;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.solver.MonopivotSolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.EnumSet;

@Service
public class KinematicsService {

    private final MonopivotSolver solver = new MonopivotSolver();

    /** Turns the request (points in pixels + calibration) into the engine's input (points in mm). */
    private KinematicsInput toKinematicsInput(PreviewRequest request) {
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

        List<MarkedPoint> points = request.points().stream()
                .map(dto -> new MarkedPoint(dto.type(),
                        new Point2D(dto.x() * mmPerPixel, dto.y() * mmPerPixel)))
                .toList();

        KinematicsParametersDto params = request.parameters();
        KinematicsParameters parameters = new KinematicsParameters(
                params.shockStrokeMm(), params.chainringTeeth(), params.sprocketTeeth(),
                params.declaredTravelMm(), params.sagPercent());

        return new KinematicsInput(points, parameters);
    }

    private Point2D pixelPointOf(List<PointDto> points, PointType type) {
        for (PointDto point : points) {
            if (point.type() == type) {
                return new Point2D(point.x(), point.y());
            }
        }
        throw new IllegalArgumentException("Missing point: " + type);
    }

    public PreviewResponse preview(PreviewRequest request) {
        KinematicsInput input = toKinematicsInput(request);
        KinematicsParametersDto parameters = request.parameters();

        List<Point2D> axlePath = solver.sweep(input);

        LeverageCurve leverageCurve = LeverageCurve.from(axlePath, parameters.shockStrokeMm());
        KickbackCurve kickbackCurve = KickbackCurve.from(
                axlePath, input.pointOf(PointType.BOTTOM_BRACKET), parameters.chainringTeeth());

        LeverageDescriptors leverageDescriptors = LeverageDescriptors.from(leverageCurve, parameters.sagPercent());
        AxlePathDescriptors axlePathDescriptors = AxlePathDescriptors.from(axlePath);
        TravelCheck travelCheck = TravelCheck.from(axlePath, parameters.declaredTravelMm());

        MeasurementConditions conditions = new MeasurementConditions(
                parameters.sagPercent(), parameters.chainringTeeth(), parameters.sprocketTeeth());

        return new PreviewResponse(
                leverageCurve.samples(),
                kickbackCurve.samples(),
                axlePath,
                leverageDescriptors,
                axlePathDescriptors,
                travelCheck,
                conditions);
    }
}
