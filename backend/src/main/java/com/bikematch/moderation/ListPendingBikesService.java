package com.bikematch.moderation;

import com.bikematch.bike.BikeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPendingBikesService {

    private final BikeRepository bikeRepository;

    public ListPendingBikesService(BikeRepository bikeRepository) {
        this.bikeRepository = bikeRepository;
    }

    @Transactional(readOnly = true)
    public List<PendingBikeSummary> list() {
        return bikeRepository.findPendingSummaries();
    }
}
