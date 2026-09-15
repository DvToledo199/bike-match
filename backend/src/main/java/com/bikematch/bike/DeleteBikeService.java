package com.bikematch.bike;

import com.bikematch.media.ImageStorage;
import com.bikematch.media.ImageStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Permanently deletes a bike. Its database rows (bike, marked points, saved result and
 * explanations) are removed first, in one transaction; the photo is removed from image
 * storage only after that commit, so a storage failure never leaves a half-deleted bike.
 */
@Service
public class DeleteBikeService {

    private static final Logger log = LoggerFactory.getLogger(DeleteBikeService.class);

    private final BikeRepository bikeRepository;
    private final ImageStorage imageStorage;
    private final TransactionTemplate transaction;

    public DeleteBikeService(
            BikeRepository bikeRepository,
            ImageStorage imageStorage,
            PlatformTransactionManager transactionManager
    ) {
        this.bikeRepository = bikeRepository;
        this.imageStorage = imageStorage;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public void deleteOwnedBike(long ownerId, long bikeId) {
        Boolean photoStored = transaction.execute(status -> {
            Bike bike = bikeRepository.findOwnedByIdForUpdate(bikeId, ownerId)
                    .orElseThrow(BikeNotFoundException::new);
            boolean hasPhoto = bike.getPhotoUrl() != null;
            bikeRepository.deleteBikeById(bikeId);
            return hasPhoto;
        });
        if (Boolean.TRUE.equals(photoStored)) {
            deletePhoto(bikeId);
        }
    }

    private void deletePhoto(long bikeId) {
        try {
            imageStorage.delete(BikePhotos.publicId(bikeId));
        } catch (ImageStorageException exception) {
            log.warn("Bike {} was deleted but its photo is still in image storage", bikeId);
        }
    }
}
