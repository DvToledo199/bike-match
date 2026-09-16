package com.bikematch.kinematics.model;

import com.bikematch.kinematics.curve.CurveChecks;
import com.bikematch.kinematics.geometry.ChainDrive;
import com.bikematch.kinematics.geometry.Point2D;

import java.util.Objects;

/** Inputs shared by the five reference curves for one solved Horst-link. */
public record HorstLinkCurveInput(
        HorstLinkGeometry geometry,
        Point2D bottomBracket,
        Point2D frontAxle,
        KinematicsParameters parameters,
        ReferenceSetup referenceSetup) {

    public HorstLinkCurveInput {
        Objects.requireNonNull(geometry, "Horst-link geometry is required");
        requireFinitePoint(bottomBracket, "Bottom bracket");
        requireFinitePoint(frontAxle, "Front axle");
        Objects.requireNonNull(parameters, "Kinematics parameters are required");
        Objects.requireNonNull(referenceSetup, "Reference setup is required");
        CurveChecks.positiveFinite(parameters.shockStrokeMm(), "Shock stroke");
        CurveChecks.positiveFinite(parameters.chainringTeeth(), "Chainring teeth");
        CurveChecks.positiveFinite(parameters.sprocketTeeth(), "Sprocket teeth");
    }

    public ChainDrive chainDrive() {
        return new ChainDrive(parameters.chainringTeeth(), parameters.sprocketTeeth());
    }

    private static void requireFinitePoint(Point2D point, String name) {
        Objects.requireNonNull(point, name + " is required");
        if (!Double.isFinite(point.x()) || !Double.isFinite(point.y())) {
            throw new IllegalArgumentException(name + " must have finite coordinates");
        }
    }
}
