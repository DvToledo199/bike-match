package com.bikematch.bike;

import com.bikematch.kinematics.model.PointType;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

public record MarkedPhotoGeometry(
        int schemaVersion,
        int imageWidth,
        int imageHeight,
        List<MarkedPhotoPoint> points
) {

    public static final int CURRENT_SCHEMA_VERSION = 1;
    private static final int MAXIMUM_IMAGE_SIDE_PIXELS = 100_000;

    public MarkedPhotoGeometry {
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported marked-photo schema version");
        }
        requireImageDimension(imageWidth, "Image width");
        requireImageDimension(imageHeight, "Image height");
        points = List.copyOf(Objects.requireNonNull(points, "Marked points are required"));

        EnumSet<PointType> pointTypes = EnumSet.noneOf(PointType.class);
        for (MarkedPhotoPoint point : points) {
            Objects.requireNonNull(point, "A marked point is missing");
            if (!pointTypes.add(point.type())) {
                throw new IllegalArgumentException("Duplicate point: " + point.type());
            }
            if (point.x() > imageWidth || point.y() > imageHeight) {
                throw new IllegalArgumentException(
                        "Point " + point.type() + " must be inside the original image");
            }
        }
        if (!pointTypes.equals(EnumSet.allOf(PointType.class))) {
            throw new IllegalArgumentException("Mark each of the six required points exactly once");
        }
    }

    public static MarkedPhotoGeometry create(
            int imageWidth,
            int imageHeight,
            List<MarkedPhotoPoint> points
    ) {
        return new MarkedPhotoGeometry(
                CURRENT_SCHEMA_VERSION, imageWidth, imageHeight, points);
    }

    private static void requireImageDimension(int value, String name) {
        if (value < 1 || value > MAXIMUM_IMAGE_SIDE_PIXELS) {
            throw new IllegalArgumentException(
                    name + " must be between 1 and " + MAXIMUM_IMAGE_SIDE_PIXELS + " pixels");
        }
    }
}
