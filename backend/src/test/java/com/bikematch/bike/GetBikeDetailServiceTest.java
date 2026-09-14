package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetBikeDetailServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private Bike bike;

    @Mock
    private User owner;

    @Mock
    private KinematicsResult result;

    private GetBikeDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetBikeDetailService(bikeRepository, new ObjectMapper());
        given(bikeRepository.findById(7L)).willReturn(Optional.of(bike));
        given(bike.getOwner()).willReturn(owner);
        given(owner.getUsername()).willReturn("david");
        given(bike.getId()).willReturn(7L);
        given(bike.getBrand()).willReturn("Orange");
        given(bike.getModel()).willReturn("Stage 6");
        given(bike.getModelYear()).willReturn((short) 2020);
        given(bike.getCategory()).willReturn(BikeCategory.ENDURO);
        given(bike.getSuspensionLayout()).willReturn(SuspensionLayout.SINGLE_PIVOT);
        given(bike.getDeclaredTravelMm()).willReturn(150D);
        given(bike.getShockEyeToEyeMm()).willReturn(230D);
        given(bike.getShockStrokeMm()).willReturn(65D);
        given(bike.getWheelConfiguration()).willReturn(WheelConfiguration.FULL_29);
        given(bike.getCassetteType()).willReturn(CassetteType.TWELVE_SPEED);
        given(bike.getChainringTeeth()).willReturn((short) 32);
        given(bike.getSprocketTeeth()).willReturn((short) 50);
        given(bike.getSagPercent()).willReturn(30D);
        given(bike.getPhotoUrl()).willReturn("https://example.com/bike.jpg");
        given(bike.getStatus()).willReturn(BikeStatus.PUBLIC);
        given(bike.getCreatedAt()).willReturn(Instant.parse("2026-09-14T12:00:00Z"));
        given(bike.getKinematicsResult()).willReturn(result);
        given(result.getResultVersion()).willReturn(1);
        given(result.getEngineVersion()).willReturn("monopivot-reference-v2");
        given(result.getCurves()).willReturn("{\"leverageCurve\":[]}");
        given(result.getDescriptors()).willReturn("{\"travelCheck\":{\"withinTolerance\":true}}");
        given(result.getCapabilities()).willReturn("{\"antiSquat\":true}");
        given(result.getComputedAt()).willReturn(Instant.parse("2026-09-14T12:01:00Z"));
    }

    @Test
    void returnsPublicBikeAndParsesStoredResultJson() {
        given(bike.canBeViewedBy(null)).willReturn(true);

        GetBikeDetailService.BikeDetail detail = service.get(7L, null);

        assertThat(detail.ownerUsername()).isEqualTo("david");
        assertThat(detail.result().curves().get("leverageCurve")).isEmpty();
        assertThat(detail.result().descriptors().path("travelCheck").path("withinTolerance")
                .asBoolean()).isTrue();
        assertThat(detail.result().capabilities().path("antiSquat").asBoolean()).isTrue();
    }

    @Test
    void hidesPrivateBikeFromAnotherViewerAsNotFound() {
        given(bike.canBeViewedBy(99L)).willReturn(false);

        assertThatThrownBy(() -> service.get(7L, 99L))
                .isInstanceOf(BikeNotFoundException.class);
    }

    @Test
    void allowsTheOwnerToReadTheirPrivateBike() {
        given(bike.canBeViewedBy(42L)).willReturn(true);
        given(bike.getStatus()).willReturn(BikeStatus.PRIVATE);

        GetBikeDetailService.BikeDetail detail = service.get(7L, 42L);

        assertThat(detail.status()).isEqualTo(BikeStatus.PRIVATE);
    }
}
