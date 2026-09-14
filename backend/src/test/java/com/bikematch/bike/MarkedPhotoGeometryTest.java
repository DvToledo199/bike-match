package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bikematch.kinematics.model.PointType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MarkedPhotoGeometryTest {

    @Test
    void acceptsEveryRequiredPointInsideTheOriginalImage() {
        MarkedPhotoGeometry geometry = geometry(validPoints());

        assertThat(geometry.schemaVersion()).isEqualTo(1);
        assertThat(geometry.imageWidth()).isEqualTo(1800);
        assertThat(geometry.imageHeight()).isEqualTo(1200);
        assertThat(geometry.points()).hasSize(6);
    }

    @Test
    void keepsADefensiveCopyOfThePointList() {
        List<MarkedPhotoPoint> points = new ArrayList<>(validPoints());
        MarkedPhotoGeometry geometry = geometry(points);

        points.clear();

        assertThat(geometry.points()).hasSize(6);
        assertThatThrownBy(() -> geometry.points().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateOrMissingPointTypes() {
        List<MarkedPhotoPoint> points = new ArrayList<>(validPoints());
        points.set(5, points.get(0));

        assertThatThrownBy(() -> geometry(points))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate point: MAIN_PIVOT");
    }

    @Test
    void rejectsPointsOutsideTheOriginalImage() {
        List<MarkedPhotoPoint> points = new ArrayList<>(validPoints());
        points.set(0, new MarkedPhotoPoint(PointType.MAIN_PIVOT, 1801, 500));

        assertThatThrownBy(() -> geometry(points))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Point MAIN_PIVOT must be inside the original image");
    }

    private MarkedPhotoGeometry geometry(List<MarkedPhotoPoint> points) {
        return MarkedPhotoGeometry.create(1800, 1200, points);
    }

    private List<MarkedPhotoPoint> validPoints() {
        return List.of(
                new MarkedPhotoPoint(PointType.MAIN_PIVOT, 805, 796),
                new MarkedPhotoPoint(PointType.SHOCK_FRAME, 923, 640),
                new MarkedPhotoPoint(PointType.SHOCK_SWINGARM, 760, 661),
                new MarkedPhotoPoint(PointType.BOTTOM_BRACKET, 778, 855),
                new MarkedPhotoPoint(PointType.REAR_AXLE, 409, 826),
                new MarkedPhotoPoint(PointType.FRONT_AXLE, 1433, 826));
    }
}
