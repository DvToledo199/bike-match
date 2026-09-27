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
class HorstLinkSeatstayPersistenceTest {
    @Autowired private UserRepository users;
    @Autowired private BikeRepository bikes;
    @Autowired private KinematicsResultRepository results;
    @Autowired private EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {"horst-link-seatstay-v1", "horst-link-seatstay-reference-v1"})
    void migrationAcceptsTheSeatstayDrivenLayoutAndItsEngineVersions(String version) {
        User owner = users.saveAndFlush(new User(
                "seatstay-test@example.com", "seatstay_test", "test-hash", Role.USER));
        Bike bike = bikes.saveAndFlush(Bike.createPrivate(owner, new BikeDetails(
                "Test", "Synthetic seatstay drive", null, BikeCategory.ENDURO,
                SuspensionLayout.HORST_LINK_SEATSTAY, 160, 230, 65, WheelConfiguration.MULLET,
                CassetteType.TWELVE_SPEED, (short) 34, (short) 52, 30)));
        Long bikeId = bike.getId();
        bike.attachPhoto(URI.create("https://example.com/synthetic-seatstay-drive.jpg"));
        bike.attachLinkagePoints(MarkedPhotoGeometry.create(1800, 1200, List.of(
                new MarkedPhotoPoint(PointType.MAIN_PIVOT, 556, 512),
                new MarkedPhotoPoint(PointType.HORST_PIVOT, 344, 546),
                new MarkedPhotoPoint(PointType.ROCKER_FRAME_PIVOT, 604, 427),
                new MarkedPhotoPoint(PointType.ROCKER_SEATSTAY_PIVOT, 563, 414),
                new MarkedPhotoPoint(PointType.SHOCK_FRAME, 729, 333),
                new MarkedPhotoPoint(PointType.SHOCK_SEATSTAY, 605, 394),
                new MarkedPhotoPoint(PointType.BOTTOM_BRACKET, 578, 547),
                new MarkedPhotoPoint(PointType.REAR_AXLE, 311, 543),
                new MarkedPhotoPoint(PointType.FRONT_AXLE, 1076, 533)), SuspensionLayout.HORST_LINK_SEATSTAY));
        bikes.saveAndFlush(bike);
        results.saveAndFlush(new KinematicsResult(bike, 1, version, "{}", "{}", "{}"));
        entityManager.clear();

        Bike stored = bikes.findById(bikeId).orElseThrow();
        assertThat(stored.getSuspensionLayout()).isEqualTo(SuspensionLayout.HORST_LINK_SEATSTAY);
        assertThat(stored.getLinkagePoints().suspensionLayout()).isEqualTo(SuspensionLayout.HORST_LINK_SEATSTAY);
        assertThat(results.findByBikeId(bikeId).orElseThrow().getEngineVersion()).isEqualTo(version);
    }
}
