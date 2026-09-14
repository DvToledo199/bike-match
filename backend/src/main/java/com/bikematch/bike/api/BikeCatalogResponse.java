package com.bikematch.bike.api;

import com.bikematch.bike.ListPublicBikesService;
import com.bikematch.bike.PublicBikeSummary;
import java.util.List;
import org.springframework.data.domain.Page;

public record BikeCatalogResponse(
        List<PublicBikeSummaryResponse> items,
        int page,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static BikeCatalogResponse from(Page<PublicBikeSummary> result) {
        return new BikeCatalogResponse(
                result.getContent().stream().map(PublicBikeSummaryResponse::from).toList(),
                result.getNumber(),
                ListPublicBikesService.PAGE_SIZE,
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext());
    }
}
