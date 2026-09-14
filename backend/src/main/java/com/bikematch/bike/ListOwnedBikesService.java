package com.bikematch.bike;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListOwnedBikesService {

    private final BikeRepository bikeRepository;

    public ListOwnedBikesService(BikeRepository bikeRepository) {
        this.bikeRepository = bikeRepository;
    }

    @Transactional(readOnly = true)
    public List<OwnedBikeSummary> list(long ownerId) {
        return bikeRepository.findSummariesByOwnerId(ownerId);
    }
}
