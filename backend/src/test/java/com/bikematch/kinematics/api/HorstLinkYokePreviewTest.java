package com.bikematch.kinematics.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorstLinkYokePreviewTest {

    private final KinematicsService service = new KinematicsService();

    @Test
    void calculatesAllFiveReferenceCurvesUsingTheRealYokeShockEye() {
        PreviewResponse response = service.preview(request(WheelConfiguration.FULL_29));

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-yoke-reference-v2");
        assertThat(response.conditions().reference().brakeModel()).isEqualTo("SEATSTAY_FIXED");
        assertThat(response.leverageCurve()).hasSize(100);
        assertThat(response.kickbackCurve()).hasSize(101);
        assertThat(response.axlePath()).hasSize(101);
        assertThat(response.antiSquatCurve()).hasSize(101);
        assertThat(response.antiRiseCurve()).hasSize(101);
        assertThat(response.antiSquatCurve()).allSatisfy(sample ->
                assertThat(sample.percent()).isFinite());
        assertThat(response.antiRiseCurve()).allSatisfy(sample ->
                assertThat(sample.percent()).isFinite());
    }

    @Test
    void usesTheBasicYokeEngineWhenTheWheelReferenceIsUnavailable() {
        PreviewResponse response = service.preview(request(null));

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-yoke-v2");
        assertThat(response.conditions().reference()).isNull();
        assertThat(response.antiSquatCurve()).isEmpty();
        assertThat(response.antiRiseCurve()).isEmpty();
    }

    @Test
    void preservesResultsWhenThePhotoIsMirroredOrResized() {
        PreviewRequest original = request(WheelConfiguration.FULL_29);
        PreviewResponse expected = service.preview(original);
        List<PointDto> mirroredPoints = original.points().stream()
                .map(point -> new PointDto(point.type(), 5000 - point.x() * 1.5, point.y() * 1.5))
                .toList();
        PreviewResponse mirrored = service.preview(new PreviewRequest(
                mirroredPoints, original.eyeToEyeMm(), original.parameters(), original.suspensionLayout()));
        assertThat(mirrored).usingRecursiveComparison()
                .withComparatorForType((a, b) -> Math.abs(a - b) < 1e-7 ? 0 : Double.compare(a, b), Double.class)
                .isEqualTo(expected);
    }

    @Test
    void calibratesFromThePhysicalShockEyeRatherThanTheYokeRockerPivot() {
        PreviewResponse yokeResponse = service.preview(request(WheelConfiguration.FULL_29));
        PreviewResponse directResponse = service.preview(new PreviewRequest(
                directPoints(), Math.hypot(350, 150), parameters(WheelConfiguration.FULL_29),
                SuspensionLayout.HORST_LINK));

        assertThat(yokeResponse).usingRecursiveComparison()
                .ignoringFields("conditions.modelVersion")
                .withComparatorForType((a, b) -> Math.abs(a - b) < 1e-7 ? 0 : Double.compare(a, b), Double.class)
                .isEqualTo(directResponse);
    }

    @Test
    void rejectsYokePointsWhenTheRequestClaimsToBeDirectHorstGeometry() {
        assertThatThrownBy(() -> service.preview(new PreviewRequest(
                points(), 210.0, parameters(WheelConfiguration.FULL_29), SuspensionLayout.HORST_LINK)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mark each of the 9 required points exactly once");
    }

    private PreviewRequest request(WheelConfiguration wheels) {
        return new PreviewRequest(points(), 210.0, parameters(wheels), SuspensionLayout.HORST_LINK_YOKE);
    }

    private KinematicsParametersDto parameters(WheelConfiguration wheels) {
        return new KinematicsParametersDto(55.0, 32, 51, 150.0, 25.0, wheels);
    }

    private List<PointDto> points() {
        double fraction = 210 / Math.hypot(350, 150);
        return List.of(
                point(PointType.MAIN_PIVOT, 0, 350),
                point(PointType.HORST_PIVOT, -100, 350),
                point(PointType.ROCKER_FRAME_PIVOT, 0, 0),
                point(PointType.ROCKER_SEATSTAY_PIVOT, -100, 0),
                point(PointType.SHOCK_FRAME, 250, -150),
                point(PointType.YOKE_ROCKER_PIVOT, -100, 0),
                point(PointType.SHOCK_YOKE_EYE, 250 - 350 * fraction, -150 + 150 * fraction),
                point(PointType.BOTTOM_BRACKET, 250, 350),
                point(PointType.REAR_AXLE, -160, 320),
                point(PointType.FRONT_AXLE, 1100, 320));
    }

    private List<PointDto> directPoints() {
        return points().stream()
                .filter(point -> point.type() != PointType.SHOCK_YOKE_EYE)
                .map(point -> point.type() == PointType.YOKE_ROCKER_PIVOT
                        ? new PointDto(PointType.SHOCK_ROCKER, point.x(), point.y()) : point)
                .toList();
    }

    private PointDto point(PointType type, double x, double y) {
        return new PointDto(type, 1000 + x * 2, 600 + y * 2);
    }
}
