package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.List;
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

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesABikeAndItsCurrentKinematicsResult() throws Exception {
        User owner = userRepository.saveAndFlush(new User(
                "bike-owner@example.com", "bikeowner", "password-hash", Role.USER));
        BikeDetails details = new BikeDetails(
                "Orange", "Stage 6", (short) 2020, BikeCategory.ENDURO,
                SuspensionLayout.SINGLE_PIVOT, 150, 230, 65,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30);
        Bike bike = bikeRepository.saveAndFlush(Bike.createPrivate(owner, details));
        bike.attachPhoto(URI.create(
                "https://res.cloudinary.com/demo/image/upload/bikematch/bikes/7.jpg"));
        MarkedPhotoGeometry geometry = MarkedPhotoGeometry.create(1800, 1200, List.of(
                new MarkedPhotoPoint(PointType.MAIN_PIVOT, 805, 796),
                new MarkedPhotoPoint(PointType.SHOCK_FRAME, 923, 640),
                new MarkedPhotoPoint(PointType.SHOCK_SWINGARM, 760, 661),
                new MarkedPhotoPoint(PointType.BOTTOM_BRACKET, 778, 855),
                new MarkedPhotoPoint(PointType.REAR_AXLE, 409, 826),
                new MarkedPhotoPoint(PointType.FRONT_AXLE, 1433, 826)));
        bike.attachLinkagePoints(geometry);
        bikeRepository.saveAndFlush(bike);

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
        assertThat(bike.getPhotoUrl()).isEqualTo(
                "https://res.cloudinary.com/demo/image/upload/bikematch/bikes/7.jpg");
        assertThat(bike.getLinkagePoints()).isEqualTo(geometry);
        assertThat(bike.getKinematicsResult()).isSameAs(result);

        entityManager.clear();
        Bike reloaded = bikeRepository.findByIdAndOwnerId(bike.getId(), owner.getId())
                .orElseThrow();
        assertThat(reloaded.getLinkagePoints()).isEqualTo(geometry);
        assertThat(bikeRepository.findOwnedByIdForUpdate(bike.getId(), owner.getId()))
                .contains(reloaded);
        JsonNode capabilities = new ObjectMapper().readTree(
                reloaded.getKinematicsResult().getCapabilities());
        assertThat(capabilities.get("antiSquat").asBoolean()).isTrue();
        assertThat(capabilities.get("antiRise").asBoolean()).isTrue();
    }
}
