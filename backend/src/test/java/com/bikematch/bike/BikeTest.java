package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import java.net.URI;
import org.junit.jupiter.api.Test;

class BikeTest {

    private static final URI FIRST_PHOTO = URI.create("https://example.com/first-bike.jpg");
    private static final URI SECOND_PHOTO = URI.create("https://example.com/second-bike.jpg");

    @Test
    void draftBikeMayReplaceItsPhoto() {
        Bike bike = bike();
        bike.attachPhoto(FIRST_PHOTO);

        bike.attachPhoto(SECOND_PHOTO);

        assertThat(bike.getPhotoUrl()).isEqualTo(SECOND_PHOTO.toString());
        assertThat(bike.isAnalyzed()).isFalse();
    }

    @Test
    void analyzedBikeCannotReplaceItsPhoto() {
        Bike bike = bike();
        bike.attachPhoto(FIRST_PHOTO);
        new KinematicsResult(bike, 1, "monopivot-reference-v2", "{}", "{}", "{}");

        assertThatThrownBy(() -> bike.attachPhoto(SECOND_PHOTO))
                .isInstanceOf(BikeAnalysisLockedException.class)
                .hasMessage("An analyzed bike's photo and marked points cannot be changed");
        assertThat(bike.getPhotoUrl()).isEqualTo(FIRST_PHOTO.toString());
        assertThat(bike.isAnalyzed()).isTrue();
    }

    private Bike bike() {
        User owner = new User("owner@example.com", "owner", "password-hash", Role.USER);
        BikeDetails details = new BikeDetails(
                "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                SuspensionLayout.SINGLE_PIVOT, 150, 230, 65,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30);
        return Bike.createPrivate(owner, details);
    }
}
