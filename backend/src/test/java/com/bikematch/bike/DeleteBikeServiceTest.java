package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bikematch.media.ImageStorage;
import com.bikematch.media.ImageStorageException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.PlatformTransactionManager;

class DeleteBikeServiceTest {

    private final BikeRepository bikeRepository = mock(BikeRepository.class);
    private final ImageStorage imageStorage = mock(ImageStorage.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private final DeleteBikeService service =
            new DeleteBikeService(bikeRepository, imageStorage, transactionManager);

    @Test
    void ownerDeletesTheBikeRowsBeforeItsPhoto() {
        Bike bike = bikeWithPhoto();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));

        service.deleteOwnedBike(42L, 7L);

        InOrder order = inOrder(bikeRepository, transactionManager, imageStorage);
        order.verify(bikeRepository).deleteBikeById(7L);
        order.verify(transactionManager).commit(any());
        order.verify(imageStorage).delete("bikematch/bikes/7");
    }

    @Test
    void bikeWithoutPhotoDoesNotCallImageStorage() {
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(mock(Bike.class)));

        service.deleteOwnedBike(42L, 7L);

        verify(bikeRepository).deleteBikeById(7L);
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void missingOrForeignBikeReturnsNotFoundWithoutDeletingAnything() {
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteOwnedBike(42L, 7L))
                .isInstanceOf(BikeNotFoundException.class);

        verify(bikeRepository, never()).deleteBikeById(anyLong());
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void storageFailureDoesNotUndoTheDeletedBike() {
        Bike bike = bikeWithPhoto();
        given(bikeRepository.findOwnedByIdForUpdate(7L, 42L)).willReturn(Optional.of(bike));
        willThrow(new ImageStorageException(new RuntimeException("provider down")))
                .given(imageStorage).delete("bikematch/bikes/7");

        assertThatCode(() -> service.deleteOwnedBike(42L, 7L)).doesNotThrowAnyException();

        verify(bikeRepository).deleteBikeById(7L);
    }

    private Bike bikeWithPhoto() {
        Bike bike = mock(Bike.class);
        given(bike.getPhotoUrl()).willReturn(
                "https://res.cloudinary.com/demo/image/upload/bikematch/bikes/7.webp");
        return bike;
    }
}
