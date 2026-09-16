package com.bikematch.kinematics.curve;

import com.bikematch.kinematics.geometry.Point2D;
import com.bikematch.kinematics.model.HorstLinkCurveInput;
import com.bikematch.kinematics.solver.HorstLinkPosition;

import java.util.List;
import java.util.Objects;

/** The five reference curves derived from one analytic Horst-link sweep. */
public record HorstLinkCurves(
        List<Point2D> axlePath,
        LeverageCurve leverage,
        KickbackCurve kickback,
        SuspensionResponseCurves responses) {

    public HorstLinkCurves {
        axlePath = List.copyOf(axlePath);
        Objects.requireNonNull(leverage, "Leverage curve is required");
        Objects.requireNonNull(kickback, "Kickback curve is required");
        Objects.requireNonNull(responses, "Response curves are required");
    }

    public static HorstLinkCurves from(List<HorstLinkPosition> positions, HorstLinkCurveInput input) {
        if (positions == null || positions.size() < 2) {
            throw new IllegalArgumentException("A Horst-link sweep needs at least two positions");
        }
        Objects.requireNonNull(input, "Horst-link curve input is required");
        List<Point2D> axlePath = positions.stream()
                .map(position -> {
                    Objects.requireNonNull(position, "Horst-link position is required");
                    return position.rearAxle();
                })
                .toList();
        CurveChecks.compressionPath(axlePath);

        LeverageCurve leverage = LeverageCurve.from(axlePath, input.parameters().shockStrokeMm());
        KickbackCurve kickback = KickbackCurve.from(axlePath, input.bottomBracket(), input.chainDrive(),
                input.referenceSetup().wheels().rearRadiusMm());
        SuspensionResponseCurves responses = SuspensionResponseCurves.fromHorstLink(positions, input);
        return new HorstLinkCurves(axlePath, leverage, kickback, responses);
    }
}
