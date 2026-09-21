package com.bikematch.bike;

import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import com.bikematch.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class HorstLinkYokePersistenceTest {
    @Autowired private UserRepository users;
    @Autowired private BikeRepository bikes;
    @Autowired private KinematicsResultRepository results;
    @Autowired private EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {"horst-link-yoke-v1", "horst-link-yoke-reference-v1",
            "horst-link-yoke-v2", "horst-link-yoke-reference-v2"})
    void migrationAcceptsCorrectedVersionsAndPreservesHistoricalVersions(String version) {
        User owner = users.saveAndFlush(new User("yoke-test@example.com", "yoke_test", "test-hash", Role.USER));
        Bike bike = bikes.saveAndFlush(Bike.createPrivate(owner, new BikeDetails(
                "Test", "Synthetic yoke", null, BikeCategory.ENDURO, SuspensionLayout.HORST_LINK_YOKE,
                150, 210, 55, WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 51, 25)));
        Long bikeId = bike.getId();
        bike.attachPhoto(URI.create("https://example.com/synthetic-yoke.jpg"));
        bike.attachLinkagePoints(MarkedPhotoGeometry.create(4000, 2000, List.of(
                new MarkedPhotoPoint(PointType.MAIN_PIVOT, 1000, 1300),
                new MarkedPhotoPoint(PointType.HORST_PIVOT, 800, 1300),
                new MarkedPhotoPoint(PointType.ROCKER_FRAME_PIVOT, 1000, 600),
                new MarkedPhotoPoint(PointType.ROCKER_SEATSTAY_PIVOT, 800, 600),
                new MarkedPhotoPoint(PointType.SHOCK_FRAME, 1500, 300),
                new MarkedPhotoPoint(PointType.YOKE_ROCKER_PIVOT, 800, 600),
                new MarkedPhotoPoint(PointType.SHOCK_YOKE_EYE, 1113.9590873924158, 465.44610540325044),
                new MarkedPhotoPoint(PointType.BOTTOM_BRACKET, 1500, 1300),
                new MarkedPhotoPoint(PointType.REAR_AXLE, 680, 1240),
                new MarkedPhotoPoint(PointType.FRONT_AXLE, 3200, 1240)), SuspensionLayout.HORST_LINK_YOKE));
        bikes.saveAndFlush(bike);
        results.saveAndFlush(new KinematicsResult(bike, 1, version, "{}", "{}", "{}"));
        entityManager.clear();

        assertThat(results.findByBikeId(bikeId).orElseThrow().getEngineVersion()).isEqualTo(version);
    }
}
