package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bikematch.kinematics.api.KinematicsService;
import com.bikematch.kinematics.api.PreviewResponse;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinalizeBikeAnalysisServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private KinematicsResultRepository resultRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FinalizeBikeAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new FinalizeBikeAnalysisService(
                bikeRepository, resultRepository, new KinematicsService(), objectMapper);
    }

    @Test
    void calculatesAndStoresSourceAndResultAsOneAnalysis() throws Exception {
        Bike bike = bikeWithPhoto();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));

        PreviewResponse response = service.finalizeAnalysis(42L, 7L, validGeometry());

        assertThat(response.conditions().modelVersion()).isEqualTo("monopivot-reference-v2");
        assertThat(bike.isAnalyzed()).isTrue();
        assertThat(bike.getLinkagePoints()).isEqualTo(validGeometry());
        verify(bikeRepository).save(bike);

        ArgumentCaptor<KinematicsResult> resultCaptor =
                ArgumentCaptor.forClass(KinematicsResult.class);
        verify(resultRepository).saveAndFlush(resultCaptor.capture());
        KinematicsResult result = resultCaptor.getValue();
        assertThat(result.getResultVersion()).isEqualTo(1);
        assertThat(result.getEngineVersion()).isEqualTo("monopivot-reference-v2");

        JsonNode curves = objectMapper.readTree(result.getCurves());
        JsonNode descriptors = objectMapper.readTree(result.getDescriptors());
        JsonNode capabilities = objectMapper.readTree(result.getCapabilities());
        assertThat(curves.get("leverageCurve").size()).isGreaterThan(2);
        assertThat(curves.get("antiSquatCurve").size()).isGreaterThan(2);
        assertThat(descriptors.get("conditions").get("modelVersion").asText())
                .isEqualTo("monopivot-reference-v2");
        assertThat(capabilities.get("cogAwareKickback").asBoolean()).isTrue();
        assertThat(capabilities.get("antiSquat").asBoolean()).isTrue();
        assertThat(capabilities.get("antiRise").asBoolean()).isTrue();
        assertThat(capabilities.get("referenceOnly").asBoolean()).isTrue();
    }

    @Test
    void calculationFailureLeavesThePhotoSourceEditable() {
        Bike bike = bikeWithPhoto();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));

        assertThatThrownBy(() -> service.finalizeAnalysis(42L, 7L, invalidGeometry()))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(bike.isAnalyzed()).isFalse();
        assertThat(bike.getLinkagePoints()).isNull();
        verify(bikeRepository, never()).save(any(Bike.class));
        verifyNoInteractions(resultRepository);
    }

    @Test
    void photoIsRequiredBeforeCalculation() {
        Bike bike = bike();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));

        assertThatThrownBy(() -> service.finalizeAnalysis(42L, 7L, validGeometry()))
                .isInstanceOf(BikeAnalysisNotReadyException.class)
                .hasMessage("Attach a bike photo before saving its analysis");

        assertThat(bike.getLinkagePoints()).isNull();
        verify(bikeRepository, never()).save(any(Bike.class));
        verifyNoInteractions(resultRepository);
    }

    @Test
    void hidesBikesThatDoNotBelongToTheAuthenticatedUser() {
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.finalizeAnalysis(42L, 7L, validGeometry()))
                .isInstanceOf(BikeNotFoundException.class);

        verifyNoInteractions(resultRepository);
    }

    @Test
    void calculatesAndStoresAHorstLinkBikeWithItsOwnEngineVersion() {
        Bike bike = horstBikeWithPhoto();
        MarkedPhotoGeometry geometry = horstGeometry();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));

        PreviewResponse response = service.finalizeAnalysis(42L, 7L, geometry);

        assertThat(response.conditions().modelVersion()).isEqualTo("horst-link-reference-v1");
        assertThat(response.conditions().reference().brakeModel()).isEqualTo("SEATSTAY_FIXED");
        assertThat(bike.getLinkagePoints()).isEqualTo(geometry);

        ArgumentCaptor<KinematicsResult> resultCaptor =
                ArgumentCaptor.forClass(KinematicsResult.class);
        verify(resultRepository).saveAndFlush(resultCaptor.capture());
        assertThat(resultCaptor.getValue().getEngineVersion()).isEqualTo("horst-link-reference-v1");
    }

    @Test
    void rejectsMarkedPointsForAnotherSuspensionLayoutBeforeSaving() {
        Bike bike = bikeWithPhoto();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));

        assertThatThrownBy(() -> service.finalizeAnalysis(42L, 7L, horstGeometry()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Marked points do not match this bike's suspension layout");

        assertThat(bike.isAnalyzed()).isFalse();
        verify(bikeRepository, never()).save(any(Bike.class));
        verifyNoInteractions(resultRepository);
    }

    private Bike bikeWithPhoto() {
        Bike bike = bike();
        bike.attachPhoto(URI.create("https://example.com/bike.jpg"));
        return bike;
    }

    private Bike bike() {
        User owner = new User("owner@example.com", "owner", "password-hash", Role.USER);
        BikeDetails details = new BikeDetails(
                "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                SuspensionLayout.SINGLE_PIVOT, 150, 210, 55,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30);
        return Bike.createPrivate(owner, details);
    }

    private Bike horstBikeWithPhoto() {
        User owner = new User("owner@example.com", "owner", "password-hash", Role.USER);
        BikeDetails details = new BikeDetails(
                "Reference", "Horst", (short) 2020, BikeCategory.ENDURO,
                SuspensionLayout.HORST_LINK, 100, 200, 20,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 52, 30);
        Bike bike = Bike.createPrivate(owner, details);
        bike.attachPhoto(URI.create("https://example.com/horst-bike.jpg"));
        return bike;
    }

    private MarkedPhotoGeometry validGeometry() {
        return MarkedPhotoGeometry.create(1800, 1200, List.of(
                new MarkedPhotoPoint(PointType.MAIN_PIVOT, 804.9, 795.8),
                new MarkedPhotoPoint(PointType.SHOCK_FRAME, 922.5, 640.0),
                new MarkedPhotoPoint(PointType.SHOCK_SWINGARM, 760.4, 660.6),
                new MarkedPhotoPoint(PointType.BOTTOM_BRACKET, 778.4, 855.4),
                new MarkedPhotoPoint(PointType.REAR_AXLE, 409.0, 826.1),
                new MarkedPhotoPoint(PointType.FRONT_AXLE, 1432.5, 826.1)));
    }

    private MarkedPhotoGeometry invalidGeometry() {
        List<MarkedPhotoPoint> points = new ArrayList<>(validGeometry().points());
        points.set(5, new MarkedPhotoPoint(PointType.FRONT_AXLE, 409.0, 1000.0));
        return MarkedPhotoGeometry.create(1800, 1200, points);
    }

    private MarkedPhotoGeometry horstGeometry() {
        double scale = 200 / Math.hypot(90, 40);
        return MarkedPhotoGeometry.create(4000, 2000, List.of(
                point(PointType.MAIN_PIVOT, 0, 0, scale),
                point(PointType.HORST_PIVOT, -100, 0, scale),
                point(PointType.ROCKER_FRAME_PIVOT, 0, -120, scale),
                point(PointType.ROCKER_SEATSTAY_PIVOT, -100, -120, scale),
                point(PointType.SHOCK_FRAME, 50, -160, scale),
                point(PointType.SHOCK_ROCKER, -40, -120, scale),
                point(PointType.BOTTOM_BRACKET, 250, -30, scale),
                point(PointType.REAR_AXLE, -160, -90, scale),
                point(PointType.FRONT_AXLE, 1100, 0, scale)), SuspensionLayout.HORST_LINK);
    }

    private MarkedPhotoPoint point(PointType type, double x, double y, double scale) {
        return new MarkedPhotoPoint(type, 1000 + x * scale, 600 + y * scale);
    }
}
