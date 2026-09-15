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
import java.util.function.Consumer;
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
        Bike bike = bikeOwnedBy42WithPhoto();
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));

        service.deleteOwnedBike(42L, 7L);

        InOrder order = inOrder(bikeRepository, transactionManager, imageStorage);
        order.verify(bikeRepository).deleteBikeById(7L);
        order.verify(transactionManager).commit(any());
        order.verify(imageStorage).delete("bikematch/bikes/7");
    }

    @Test
    void bikeWithoutPhotoDoesNotCallImageStorage() {
        Bike bike = mock(Bike.class);
        given(bike.isOwnedBy(42L)).willReturn(true);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));

        service.deleteOwnedBike(42L, 7L);

        verify(bikeRepository).deleteBikeById(7L);
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void missingBikeReturnsNotFoundWithoutDeletingAnything() {
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteOwnedBike(42L, 7L))
                .isInstanceOf(BikeNotFoundException.class);

        verify(bikeRepository, never()).deleteBikeById(anyLong());
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void anotherUsersBikeIsReportedAsNotFoundAndRolledBack() {
        Bike bike = bikeOwnedBy42WithPhoto();
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));

        assertThatThrownBy(() -> service.deleteOwnedBike(99L, 7L))
                .isInstanceOf(BikeNotFoundException.class);

        verify(transactionManager).rollback(any());
        verify(bikeRepository, never()).deleteBikeById(anyLong());
        verify(imageStorage, never()).delete(anyString());
    }

    @Test
    void storageFailureDoesNotUndoTheDeletedBike() {
        Bike bike = bikeOwnedBy42WithPhoto();
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));
        willThrow(new ImageStorageException(new RuntimeException("provider down")))
                .given(imageStorage).delete("bikematch/bikes/7");

        assertThatCode(() -> service.deleteOwnedBike(42L, 7L)).doesNotThrowAnyException();

        verify(bikeRepository).deleteBikeById(7L);
    }

    @Test
    void beforeDeleteRunsInsideTheTransactionBeforeTheRowsAreDeleted() {
        Bike bike = mock(Bike.class);
        given(bikeRepository.findByIdForUpdate(7L)).willReturn(Optional.of(bike));
        @SuppressWarnings("unchecked")
        Consumer<Bike> beforeDelete = mock(Consumer.class);

        service.deleteBike(7L, beforeDelete);

        InOrder order = inOrder(beforeDelete, bikeRepository, transactionManager);
        order.verify(beforeDelete).accept(bike);
        order.verify(bikeRepository).deleteBikeById(7L);
        order.verify(transactionManager).commit(any());
    }

    private Bike bikeOwnedBy42WithPhoto() {
        Bike bike = mock(Bike.class);
        given(bike.isOwnedBy(42L)).willReturn(true);
        given(bike.getPhotoUrl()).willReturn(
                "https://res.cloudinary.com/demo/image/upload/bikematch/bikes/7.webp");
        return bike;
    }
}
