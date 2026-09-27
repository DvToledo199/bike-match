package com.bikematch.kinematics.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

class HorstLinkSeatstayPreviewTest {

    private final KinematicsService service = new KinematicsService();

    @Test
    void solvesARealSeatstayDrivenBikeThatTheRockerModelCannotCompress() {
        assertThatThrownBy(() -> service.preview(request(SuspensionLayout.HORST_LINK, WheelConfiguration.MULLET)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("The rocker linkage reaches an impossible or singular position");

        PreviewResponse response = service.preview(request(SuspensionLayout.HORST_LINK_SEATSTAY,
                WheelConfiguration.MULLET));

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-seatstay-reference-v1");
        assertThat(response.conditions().reference().brakeModel()).isEqualTo("SEATSTAY_FIXED");
        assertThat(response.travelCheck().calculatedTravelMm()).isCloseTo(162, within(3.0));
        assertThat(response.travelCheck().withinTolerance()).isTrue();
        assertThat(response.leverageCurve()).hasSize(100).allSatisfy(sample ->
                assertThat(sample.ratio()).isBetween(2.0, 3.3));
        assertThat(response.axlePath()).hasSize(101);
        assertThat(response.antiSquatCurve()).hasSize(101).allSatisfy(sample ->
                assertThat(sample.percent()).isFinite());
        assertThat(response.antiRiseCurve()).hasSize(101).allSatisfy(sample ->
                assertThat(sample.percent()).isFinite());
    }

    @Test
    void usesTheBasicSeatstayEngineWhenTheWheelReferenceIsUnavailable() {
        PreviewResponse response = service.preview(request(SuspensionLayout.HORST_LINK_SEATSTAY, null));

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-seatstay-v1");
        assertThat(response.conditions().reference()).isNull();
        assertThat(response.antiSquatCurve()).isEmpty();
        assertThat(response.antiRiseCurve()).isEmpty();
    }

    @Test
    void preservesResultsWhenThePhotoIsMirroredOrResized() {
        PreviewRequest original = request(SuspensionLayout.HORST_LINK_SEATSTAY, WheelConfiguration.MULLET);
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
    void rejectsARockerShockEyeWhenTheRequestClaimsASeatstayDrivenShock() {
        PreviewRequest rockerDriven = request(SuspensionLayout.HORST_LINK, WheelConfiguration.MULLET);

        assertThatThrownBy(() -> service.preview(new PreviewRequest(rockerDriven.points(),
                rockerDriven.eyeToEyeMm(), rockerDriven.parameters(), SuspensionLayout.HORST_LINK_SEATSTAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Mark each of the 9 required points exactly once");
    }

    /**
     * Marks, in photo pixels, of a real 160 mm enduro e-bike whose seatstays drive a 230 x 65 mm
     * shock. Only the layout decides whether the shock's moving eye rides on the rocker or on the
     * seatstay.
     */
    private PreviewRequest request(SuspensionLayout layout, WheelConfiguration wheels) {
        PointType movingShockEye = layout == SuspensionLayout.HORST_LINK
                ? PointType.SHOCK_ROCKER : PointType.SHOCK_SEATSTAY;
        return new PreviewRequest(List.of(
                new PointDto(PointType.MAIN_PIVOT, 556.1817430499491, 511.8414487885076),
                new PointDto(PointType.BOTTOM_BRACKET, 578.2182929015003, 547.3945859598017),
                new PointDto(PointType.HORST_PIVOT, 344.2289727666442, 546.1215096912923),
                new PointDto(PointType.ROCKER_FRAME_PIVOT, 604.3712079010145, 426.51854518478837),
                new PointDto(PointType.ROCKER_SEATSTAY_PIVOT, 562.8765557791083, 414.01978240475955),
                new PointDto(PointType.SHOCK_FRAME, 729.0375984113531, 332.88797682126943),
                new PointDto(movingShockEye, 604.9531937570889, 393.73721031031204),
                new PointDto(PointType.REAR_AXLE, 310.5459120560171, 543.3937545931432),
                new PointDto(PointType.FRONT_AXLE, 1075.6678993742735, 532.6861453400885)),
                230.0, new KinematicsParametersDto(65.0, 34, 52, 160.0, 30.0, wheels), layout);
    }
}
