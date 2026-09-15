package com.bikematch.moderation;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikematch.bike.Bike;
import com.bikematch.bike.BikeCategory;
import com.bikematch.bike.BikeDetails;
import com.bikematch.bike.CassetteType;
import com.bikematch.bike.SuspensionLayout;
import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class BikeRemovalNoticeRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRemovalNoticeRepository noticeRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void returnsOnlyTheOwnersPendingNoticesNewestFirst() {
        User owner = userRepository.saveAndFlush(new User(
                "notice-owner@example.com", "noticeowner", "password-hash", Role.USER));
        User anotherOwner = userRepository.saveAndFlush(new User(
                "other-notice-owner@example.com", "othernoticeowner", "password-hash", Role.USER));
        noticeRepository.saveAndFlush(notice(owner, "Older bike"));
        BikeRemovalNotice dismissed = notice(owner, "Dismissed bike");
        dismissed.dismiss();
        noticeRepository.saveAndFlush(dismissed);
        BikeRemovalNotice newer = noticeRepository.saveAndFlush(notice(owner, "Newer bike"));
        BikeRemovalNotice foreign = noticeRepository.saveAndFlush(notice(anotherOwner, "Other owner's bike"));
        entityManager.clear();

        assertThat(noticeRepository.findPendingByOwnerId(owner.getId()))
                .extracting(BikeRemovalNotice::getModel)
                .containsExactly("Newer bike", "Older bike");
        assertThat(noticeRepository.findOwnedById(newer.getId(), owner.getId())).isPresent();
        assertThat(noticeRepository.findOwnedById(foreign.getId(), owner.getId())).isEmpty();
    }

    private BikeRemovalNotice notice(User owner, String model) {
        Bike bike = Bike.createPrivate(owner, new BikeDetails(
                "Orange", model, (short) 2020, BikeCategory.ENDURO,
                SuspensionLayout.SINGLE_PIVOT, 150, 230, 65,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30));
        return BikeRemovalNotice.forRemovedBike(bike, "Photo taken from another website");
    }
}
