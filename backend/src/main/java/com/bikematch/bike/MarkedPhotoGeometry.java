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

    public static final int SINGLE_PIVOT_SCHEMA_VERSION = 1;
    public static final int HORST_LINK_SCHEMA_VERSION = 2;
    public static final int HORST_LINK_YOKE_SCHEMA_VERSION = 3;
    public static final int CURRENT_SCHEMA_VERSION = HORST_LINK_YOKE_SCHEMA_VERSION;
    private static final int MAXIMUM_IMAGE_SIDE_PIXELS = 100_000;

    public MarkedPhotoGeometry {
        if (schemaVersion != SINGLE_PIVOT_SCHEMA_VERSION && schemaVersion != HORST_LINK_SCHEMA_VERSION
                && schemaVersion != HORST_LINK_YOKE_SCHEMA_VERSION) {
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
        SuspensionLayout layout = layoutFor(schemaVersion);
        if (!pointTypes.equals(layout.requiredPointTypes())) {
            throw new IllegalArgumentException("Mark each of the " + layout.requiredPointTypes().size()
                    + " required points exactly once");
        }
    }

    public static MarkedPhotoGeometry create(
            int imageWidth,
            int imageHeight,
            List<MarkedPhotoPoint> points
    ) {
        return new MarkedPhotoGeometry(
                schemaVersionFor(points), imageWidth, imageHeight, points);
    }

    public static MarkedPhotoGeometry create(
            int imageWidth,
            int imageHeight,
            List<MarkedPhotoPoint> points,
            SuspensionLayout layout
    ) {
        Objects.requireNonNull(layout, "Suspension layout is required");
        return new MarkedPhotoGeometry(schemaVersionFor(layout), imageWidth, imageHeight, points);
    }

    public SuspensionLayout suspensionLayout() {
        return layoutFor(schemaVersion);
    }

    private static int schemaVersionFor(List<MarkedPhotoPoint> points) {
        Objects.requireNonNull(points, "Marked points are required");
        boolean horstPointPresent = points.stream()
                .filter(Objects::nonNull)
                .map(MarkedPhotoPoint::type)
                .anyMatch(type -> type == PointType.HORST_PIVOT
                        || type == PointType.ROCKER_FRAME_PIVOT
                        || type == PointType.ROCKER_SEATSTAY_PIVOT
                        || type == PointType.SHOCK_ROCKER);
        boolean yokePointPresent = points.stream()
                .filter(Objects::nonNull)
                .map(MarkedPhotoPoint::type)
                .anyMatch(type -> type == PointType.YOKE_ROCKER_PIVOT
                        || type == PointType.SHOCK_YOKE_EYE);
        if (yokePointPresent) {
            return HORST_LINK_YOKE_SCHEMA_VERSION;
        }
        return horstPointPresent ? HORST_LINK_SCHEMA_VERSION : SINGLE_PIVOT_SCHEMA_VERSION;
    }

    private static int schemaVersionFor(SuspensionLayout layout) {
        return switch (layout) {
            case SINGLE_PIVOT -> SINGLE_PIVOT_SCHEMA_VERSION;
            case HORST_LINK -> HORST_LINK_SCHEMA_VERSION;
            case HORST_LINK_YOKE -> HORST_LINK_YOKE_SCHEMA_VERSION;
        };
    }

    private static SuspensionLayout layoutFor(int schemaVersion) {
        return switch (schemaVersion) {
            case SINGLE_PIVOT_SCHEMA_VERSION -> SuspensionLayout.SINGLE_PIVOT;
            case HORST_LINK_SCHEMA_VERSION -> SuspensionLayout.HORST_LINK;
            case HORST_LINK_YOKE_SCHEMA_VERSION -> SuspensionLayout.HORST_LINK_YOKE;
            default -> throw new IllegalArgumentException("Unsupported marked-photo schema version");
        };
    }

    private static void requireImageDimension(int value, String name) {
        if (value < 1 || value > MAXIMUM_IMAGE_SIDE_PIXELS) {
            throw new IllegalArgumentException(
                    name + " must be between 1 and " + MAXIMUM_IMAGE_SIDE_PIXELS + " pixels");
        }
    }
}
