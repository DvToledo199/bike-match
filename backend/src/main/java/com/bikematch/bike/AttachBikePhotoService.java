package com.bikematch.bike;

import com.bikematch.media.ImageStorage;
import java.net.URI;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttachBikePhotoService {

    private final BikeRepository bikeRepository;
    private final ImageStorage imageStorage;

    public AttachBikePhotoService(BikeRepository bikeRepository, ImageStorage imageStorage) {
        this.bikeRepository = bikeRepository;
        this.imageStorage = imageStorage;
    }

    @Transactional
    public Bike attach(long ownerId, long bikeId, BikePhotoFile photo) {
        Objects.requireNonNull(photo, "Photo is required");
        Bike bike = bikeRepository.findByIdAndOwnerId(bikeId, ownerId)
                .orElseThrow(BikeNotFoundException::new);

        URI photoUri = imageStorage.upload(
                photo.content(),
                "bikematch/bikes/" + bike.getId());
        bike.attachPhoto(photoUri);
        return bikeRepository.saveAndFlush(bike);
    }
}
