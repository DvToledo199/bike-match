package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ListPublicBikesServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Test
    void rejectsNegativePageNumbers() {
        ListPublicBikesService service = new ListPublicBikesService(bikeRepository);

        assertThatThrownBy(() -> service.list(null, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Page must be zero or greater");
    }

    @Test
    void requestsTheFixedPageSizeAndCategory() {
        given(bikeRepository.findPublicSummaries(
                BikeCategory.ENDURO,
                PageRequest.of(1, ListPublicBikesService.PAGE_SIZE)))
                .willReturn(new PageImpl<>(List.of()));
        ListPublicBikesService service = new ListPublicBikesService(bikeRepository);

        service.list(BikeCategory.ENDURO, 1);

        verify(bikeRepository).findPublicSummaries(
                BikeCategory.ENDURO,
                PageRequest.of(1, ListPublicBikesService.PAGE_SIZE));
    }
}
