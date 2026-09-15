package com.bikematch.moderation;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class RemovalNoticeServiceTest {

    private final BikeRemovalNoticeRepository noticeRepository = mock(BikeRemovalNoticeRepository.class);
    private final RemovalNoticeService service = new RemovalNoticeService(noticeRepository);

    @Test
    void ownerDismissesTheirNotice() {
        BikeRemovalNotice notice = mock(BikeRemovalNotice.class);
        given(noticeRepository.findOwnedById(3L, 42L)).willReturn(Optional.of(notice));

        service.dismiss(42L, 3L);

        verify(notice).dismiss();
        verify(noticeRepository).save(notice);
    }

    @Test
    void missingOrForeignNoticeReturnsNotFound() {
        given(noticeRepository.findOwnedById(3L, 42L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.dismiss(42L, 3L))
                .isInstanceOf(RemovalNoticeNotFoundException.class);

        verify(noticeRepository, never()).save(any());
    }
}
