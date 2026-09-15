package com.bikematch.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.bikematch.bike.Bike;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class BikeRemovalNoticeTest {

    @Test
    void keepsWhatTheOwnerNeedsOnceTheBikeIsGone() {
        Bike bike = mock(Bike.class);
        given(bike.getBrand()).willReturn("Orange");
        given(bike.getModel()).willReturn("Stage 6");

        BikeRemovalNotice notice = BikeRemovalNotice.forRemovedBike(bike, "  Offensive photo ");

        assertThat(notice.getBrand()).isEqualTo("Orange");
        assertThat(notice.getModel()).isEqualTo("Stage 6");
        assertThat(notice.getReason()).isEqualTo("Offensive photo");
        assertThat(notice.getRemovedAt()).isNotNull();
        assertThat(notice.getDismissedAt()).isNull();
    }

    @Test
    void aReasonIsRequired() {
        assertThatThrownBy(() -> BikeRemovalNotice.forRemovedBike(mock(Bike.class), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dismissingAgainKeepsTheFirstDismissal() {
        BikeRemovalNotice notice = BikeRemovalNotice.forRemovedBike(mock(Bike.class), "Offensive photo");

        notice.dismiss();
        Instant firstDismissal = notice.getDismissedAt();
        notice.dismiss();

        assertThat(firstDismissal).isNotNull();
        assertThat(notice.getDismissedAt()).isEqualTo(firstDismissal);
    }
}
