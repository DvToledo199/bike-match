package com.bikematch.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.BikeRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListPendingBikesServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    private ListPendingBikesService service;

    @BeforeEach
    void setUp() {
        service = new ListPendingBikesService(bikeRepository);
    }

    @Test
    void returnsThePendingBikeSummaries() {
        List<PendingBikeSummary> summaries = List.of(new PendingBikeSummary(
                7L, "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                "https://example.com/bike.jpg", "david", Instant.now()));
        given(bikeRepository.findPendingSummaries()).willReturn(summaries);

        assertThat(service.list()).isSameAs(summaries);

        verify(bikeRepository).findPendingSummaries();
    }
}
