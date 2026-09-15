package com.bikematch.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeNotFoundException;
import com.bikematch.bike.BikeStatus;
import com.bikematch.bike.DeleteBikeService;
import com.bikematch.user.User;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RemoveBikeServiceTest {

    private final DeleteBikeService deleteBikeService = mock(DeleteBikeService.class);
    private final BikeRemovalNoticeRepository noticeRepository = mock(BikeRemovalNoticeRepository.class);
    private final RemoveBikeService service = new RemoveBikeService(deleteBikeService, noticeRepository);

    @Test
    void deletesThePublicBikeAndLeavesItsOwnerANotice() {
        User owner = mock(User.class);
        deletionRunsWith(bike(BikeStatus.PUBLIC, owner));

        service.remove(7L, "Photo taken from another website");

        ArgumentCaptor<BikeRemovalNotice> notice = ArgumentCaptor.forClass(BikeRemovalNotice.class);
        verify(noticeRepository).save(notice.capture());
        assertThat(notice.getValue().getOwner()).isSameAs(owner);
        assertThat(notice.getValue().getModel()).isEqualTo("Stage 6");
        assertThat(notice.getValue().getReason()).isEqualTo("Photo taken from another website");
    }

    @Test
    void privateBikeIsNotFoundAndLeavesNoNotice() {
        deletionRunsWith(bike(BikeStatus.PRIVATE, mock(User.class)));

        assertThatThrownBy(() -> service.remove(7L, "Photo taken from another website"))
                .isInstanceOf(BikeNotFoundException.class);

        verify(noticeRepository, never()).save(any());
    }

    /** Makes the mocked deletion call the check-and-notice step with this locked bike. */
    private void deletionRunsWith(Bike bike) {
        willAnswer(invocation -> {
            Consumer<Bike> beforeDelete = invocation.getArgument(1);
            beforeDelete.accept(bike);
            return null;
        }).given(deleteBikeService).deleteBike(eq(7L), any());
    }

    private Bike bike(BikeStatus status, User owner) {
        Bike bike = mock(Bike.class);
        given(bike.getStatus()).willReturn(status);
        given(bike.getOwner()).willReturn(owner);
        given(bike.getBrand()).willReturn("Orange");
        given(bike.getModel()).willReturn("Stage 6");
        return bike;
    }
}
