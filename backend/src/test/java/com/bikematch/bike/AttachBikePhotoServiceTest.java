package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

import com.bikematch.media.ImageStorage;
import com.bikematch.media.ImageStorageException;
import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InOrder;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachBikePhotoServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private Bike bike;

    private AttachBikePhotoService service;

    @BeforeEach
    void setUp() {
        service = new AttachBikePhotoService(bikeRepository, imageStorage);
    }

    @Test
    void uploadsAndAttachesAPhotoToTheOwnersBike() {
        BikePhotoFile photo = validPhoto();
        URI storedPhoto = URI.create("https://res.cloudinary.com/demo/image/upload/bike.jpg");
        given(bikeRepository.findByIdAndOwnerId(7L, 42L)).willReturn(Optional.of(bike));
        given(bike.getId()).willReturn(7L);
        given(imageStorage.upload(any(byte[].class), eq("bikematch/bikes/7")))
                .willReturn(storedPhoto);
        given(bikeRepository.saveAndFlush(bike)).willReturn(bike);

        Bike result = service.attach(42L, 7L, photo);

        assertThat(result).isSameAs(bike);
        InOrder order = inOrder(bike, imageStorage);
        order.verify(bike).requireEditableAnalysisSource();
        order.verify(imageStorage).upload(any(byte[].class), eq("bikematch/bikes/7"));
        verify(bike).attachPhoto(storedPhoto);
        verify(bikeRepository).saveAndFlush(bike);
    }

    @Test
    void hidesBikesThatDoNotBelongToTheAuthenticatedUser() {
        given(bikeRepository.findByIdAndOwnerId(7L, 42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.attach(42L, 7L, validPhoto()))
                .isInstanceOf(BikeNotFoundException.class);

        verify(imageStorage, never()).upload(any(), anyString());
        verify(bikeRepository, never()).saveAndFlush(any(Bike.class));
    }

    @Test
    void storageFailureDoesNotAttachABrokenUrl() {
        given(bikeRepository.findByIdAndOwnerId(7L, 42L)).willReturn(Optional.of(bike));
        given(bike.getId()).willReturn(7L);
        given(imageStorage.upload(any(), anyString()))
                .willThrow(new ImageStorageException());

        assertThatThrownBy(() -> service.attach(42L, 7L, validPhoto()))
                .isInstanceOf(ImageStorageException.class);

        verify(bike, never()).attachPhoto(any());
        verify(bikeRepository, never()).saveAndFlush(any(Bike.class));
    }

    @Test
    void rejectsAnAnalyzedBikeBeforeOverwritingItsStoredPhoto() {
        given(bikeRepository.findByIdAndOwnerId(7L, 42L)).willReturn(Optional.of(bike));
        org.mockito.BDDMockito.willThrow(new BikeAnalysisLockedException())
                .given(bike).requireEditableAnalysisSource();

        assertThatThrownBy(() -> service.attach(42L, 7L, validPhoto()))
                .isInstanceOf(BikeAnalysisLockedException.class);

        verify(imageStorage, never()).upload(any(), anyString());
        verify(bike, never()).attachPhoto(any());
        verify(bikeRepository, never()).saveAndFlush(any(Bike.class));
    }

    private BikePhotoFile validPhoto() {
        return new BikePhotoFile(BikePhotoFileTest.jpeg(), "image/jpeg");
    }
}
