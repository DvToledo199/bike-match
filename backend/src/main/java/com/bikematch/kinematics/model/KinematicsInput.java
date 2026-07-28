package com.bikematch.kinematics.model;

import com.bikematch.kinematics.geometry.Point2D;

import java.util.List;

public record KinematicsInput(List<MarkedPoint> points, KinematicsParameters parameters) {

    public Point2D pointOf(PointType type) {

        for (MarkedPoint point : points) {
            if (type.equals(point.type())) {
                return point.position();
            }
        }
        throw new IllegalArgumentException("Missing point: " + type);

    }
}
