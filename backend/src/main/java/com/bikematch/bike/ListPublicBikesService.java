package com.bikematch.bike;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListPublicBikesService {

    public static final int PAGE_SIZE = 12;

    private final BikeRepository bikeRepository;

    public ListPublicBikesService(BikeRepository bikeRepository) {
        this.bikeRepository = bikeRepository;
    }

    @Transactional(readOnly = true)
    public Page<PublicBikeSummary> list(BikeCategory category, int pageNumber) {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }

        Pageable pageable = PageRequest.of(pageNumber, PAGE_SIZE);
        return bikeRepository.findPublicSummaries(category, pageable);
    }
}
