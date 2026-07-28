package com.bikematch.kinematics.model;

import com.bikematch.kinematics.geometry.Point2D;

/**
 * A point marked on the photo: its role plus its position in mm.
 * */
public record MarkedPoint(PointType type, Point2D position) {
}
