package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListOwnedBikesServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    private ListOwnedBikesService service;

    @BeforeEach
    void setUp() {
        service = new ListOwnedBikesService(bikeRepository);
    }

    @Test
    void returnsTheSummariesFoundForTheAuthenticatedOwner() {
        List<OwnedBikeSummary> summaries = List.of(new OwnedBikeSummary(
                7L, "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                "https://example.com/bike.jpg", BikeStatus.PRIVATE, true, Instant.now()));
        given(bikeRepository.findSummariesByOwnerId(42L)).willReturn(summaries);

        assertThat(service.list(42L)).isSameAs(summaries);

        verify(bikeRepository).findSummariesByOwnerId(42L);
    }
}
