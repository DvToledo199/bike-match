package com.bikematch.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.BikeNotPendingException;
import com.bikematch.bike.BikeRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ModerateBikePublicationServiceTest {

    private final BikeRepository bikeRepository = mock(BikeRepository.class);
    private final ModerateBikePublicationService service =
            new ModerateBikePublicationService(bikeRepository);

    @Test
    void approvesAPendingBike() {
        Bike bike = mock(Bike.class);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));
        given(bikeRepository.save(bike)).willReturn(bike);

        assertThat(service.approve(7L)).isSameAs(bike);

        verify(bike).approvePublication();
        verify(bikeRepository).save(bike);
    }

    @Test
    void rejectsAPendingBike() {
        Bike bike = mock(Bike.class);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));
        given(bikeRepository.save(bike)).willReturn(bike);

        assertThat(service.reject(7L)).isSameAs(bike);

        verify(bike).rejectPublication();
        verify(bikeRepository).save(bike);
    }

    @Test
    void missingBikeReturnsNotFound() {
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(7L))
                .isInstanceOf(BikeNotFoundException.class);

        verify(bikeRepository, never()).save(org.mockito.ArgumentMatchers.any(Bike.class));
    }

    @Test
    void completedDecisionIsNotSavedAgain() {
        Bike bike = mock(Bike.class);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));
        org.mockito.Mockito.doThrow(new BikeNotPendingException())
                .when(bike).rejectPublication();

        assertThatThrownBy(() -> service.reject(7L))
                .isInstanceOf(BikeNotPendingException.class);

        verify(bikeRepository, never()).save(bike);
    }
}
