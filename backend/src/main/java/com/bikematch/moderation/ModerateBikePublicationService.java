package com.bikematch.moderation;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.BikeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ModerateBikePublicationService {

    private final BikeRepository bikeRepository;

    public ModerateBikePublicationService(BikeRepository bikeRepository) {
        this.bikeRepository = bikeRepository;
    }

    @Transactional
    public Bike approve(long bikeId) {
        Bike bike = findBikeForModeration(bikeId);
        bike.approvePublication();
        return bikeRepository.save(bike);
    }

    @Transactional
    public Bike reject(long bikeId) {
        Bike bike = findBikeForModeration(bikeId);
        bike.rejectPublication();
        return bikeRepository.save(bike);
    }

    private Bike findBikeForModeration(long bikeId) {
        return bikeRepository.findByIdForUpdate(bikeId)
                .orElseThrow(BikeNotFoundException::new);
    }
}
