package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class PublishBikeServiceTest {

    private final BikeRepository bikeRepository = mock(BikeRepository.class);
    private final PublishBikeService service = new PublishBikeService(bikeRepository);

    @Test
    void ownerPublishesPrivateBikeForModeration() {
        Bike bike = mock(Bike.class);
        given(bike.isOwnedBy(42L)).willReturn(true);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));
        given(bikeRepository.save(bike)).willReturn(bike);

        Bike publishedBike = service.publish(42L, 7L);

        assertThat(publishedBike).isSameAs(bike);
        verify(bike).requestPublication();
        verify(bikeRepository).save(bike);
    }

    @Test
    void missingBikeReturnsNotFound() {
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(42L, 7L))
                .isInstanceOf(BikeNotFoundException.class);

        verify(bikeRepository, never()).save(org.mockito.ArgumentMatchers.any(Bike.class));
    }

    @Test
    void anotherUsersBikeReturnsForbidden() {
        Bike bike = mock(Bike.class);
        given(bike.isOwnedBy(42L)).willReturn(false);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));

        assertThatThrownBy(() -> service.publish(42L, 7L))
                .isInstanceOf(BikeAccessDeniedException.class)
                .hasMessage("You do not have permission to change this bike");

        verify(bike, never()).requestPublication();
        verify(bikeRepository, never()).save(bike);
    }
}
