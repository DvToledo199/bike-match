package com.bikematch.bike;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublishBikeService {

    private final BikeRepository bikeRepository;

    public PublishBikeService(BikeRepository bikeRepository) {
        this.bikeRepository = bikeRepository;
    }

    @Transactional
    public Bike publish(long ownerId, long bikeId) {
        Bike bike = bikeRepository.findByIdForUpdate(bikeId)
                .orElseThrow(BikeNotFoundException::new);
        if (!bike.isOwnedBy(ownerId)) {
            throw new BikeAccessDeniedException();
        }
        bike.requestPublication();
        return bikeRepository.save(bike);
    }
}
