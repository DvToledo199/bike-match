package com.bikematch.kinematics;

/**
 * A point marked on the photo: its role plus its position in mm.
 * */
public record MarkedPoint(PointType type, Point2D position) {
}
