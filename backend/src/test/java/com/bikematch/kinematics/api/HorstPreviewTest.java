package com.bikematch.kinematics.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorstPreviewTest {

    private final KinematicsService service = new KinematicsService();

    @Test
    void calculatesAllFiveReferenceCurvesForAHorstLinkPreview() {
        PreviewResponse response = service.preview(request(WheelConfiguration.FULL_29));

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-reference-v1");
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
    void usesTheBasicHorstEngineWhenTheWheelReferenceIsUnavailable() {
        PreviewResponse response = service.preview(request(null));

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-v1");
        assertThat(response.conditions().reference()).isNull();
        assertThat(response.antiSquatCurve()).isEmpty();
        assertThat(response.antiRiseCurve()).isEmpty();
    }

    @Test
    void rejectsHorstPointsWhenTheRequestClaimsToBeMonopivot() {
        assertThatThrownBy(() -> service.preview(new PreviewRequest(
                points(), 200.0, parameters(WheelConfiguration.FULL_29), SuspensionLayout.SINGLE_PIVOT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mark each of the 6 required points exactly once");
    }

    private PreviewRequest request(WheelConfiguration wheels) {
        return new PreviewRequest(points(), 200.0, parameters(wheels), SuspensionLayout.HORST_LINK);
    }

    private KinematicsParametersDto parameters(WheelConfiguration wheels) {
        return new KinematicsParametersDto(20.0, 32, 52, 100.0, 30.0, wheels);
    }

    private List<PointDto> points() {
        double scale = 200 / Math.hypot(90, 40);
        return List.of(
                point(PointType.MAIN_PIVOT, 0, 0, scale),
                point(PointType.HORST_PIVOT, -100, 0, scale),
                point(PointType.ROCKER_FRAME_PIVOT, 0, -120, scale),
                point(PointType.ROCKER_SEATSTAY_PIVOT, -100, -120, scale),
                point(PointType.SHOCK_FRAME, 50, -160, scale),
                point(PointType.SHOCK_ROCKER, -40, -120, scale),
                point(PointType.BOTTOM_BRACKET, 250, -30, scale),
                point(PointType.REAR_AXLE, -160, -90, scale),
                point(PointType.FRONT_AXLE, 1100, 0, scale));
    }

    private PointDto point(PointType type, double x, double y, double scale) {
        return new PointDto(type, 1000 + x * scale, 600 + y * scale);
    }
}
