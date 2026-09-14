package com.bikematch.bike;

import com.bikematch.kinematics.model.PointType;
import java.util.Objects;

public record MarkedPhotoPoint(PointType type, double x, double y) {

    public MarkedPhotoPoint {
        Objects.requireNonNull(type, "Point type is required");
        if (!Double.isFinite(x) || x < 0 || !Double.isFinite(y) || y < 0) {
            throw new IllegalArgumentException("Point coordinates must be finite and non-negative");
        }
    }
}
