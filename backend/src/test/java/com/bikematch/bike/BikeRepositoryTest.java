package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
class BikeRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private KinematicsResultRepository resultRepository;

    @Test
    void savesABikeAndItsCurrentKinematicsResult() {
        User owner = userRepository.saveAndFlush(new User(
                "bike-owner@example.com", "bikeowner", "password-hash", Role.USER));
        Bike bike = bikeRepository.saveAndFlush(new Bike(
                owner, "Orange", "Stage 6", (short) 2020, "ENDURO", "MONOPIVOT",
                150, 230, 65, WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30));

        KinematicsResult result = resultRepository.saveAndFlush(new KinematicsResult(
                bike, 1, "monopivot-reference-v2", "{\"leverageCurve\":[]}",
                "{\"conditions\":{\"modelVersion\":\"monopivot-reference-v2\"}}",
                "{\"antiSquat\":true,\"antiRise\":true}"));

        assertThat(bikeRepository.findByIdAndOwnerId(bike.getId(), owner.getId()))
                .contains(bike);
        assertThat(resultRepository.findByBikeId(bike.getId()))
                .contains(result);
        assertThat(result.getEngineVersion()).isEqualTo("monopivot-reference-v2");
        assertThat(bike.getStatus()).isEqualTo(BikeStatus.PRIVATE);
        assertThat(bike.getKinematicsResult()).isSameAs(result);
    }
}
