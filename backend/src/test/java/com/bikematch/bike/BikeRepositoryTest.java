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
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
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

    @Test
    void returnsOnlyTheOwnersSummariesWithDraftAndAnalysisState() {
        User owner = userRepository.saveAndFlush(new User(
                "summary-owner@example.com", "summaryowner", "password-hash", Role.USER));
        User anotherOwner = userRepository.saveAndFlush(new User(
                "another-owner@example.com", "anotherowner", "password-hash", Role.USER));
        Bike draft = bikeRepository.saveAndFlush(Bike.createPrivate(owner, details("Draft bike")));
        Bike analyzed = bikeRepository.saveAndFlush(Bike.createPrivate(owner, details("Analyzed bike")));
        bikeRepository.saveAndFlush(Bike.createPrivate(anotherOwner, details("Other owner's bike")));

        analyzed.attachPhoto(URI.create("https://example.com/analyzed-bike.jpg"));
        analyzed.attachLinkagePoints(geometry());
        bikeRepository.saveAndFlush(analyzed);
        resultRepository.saveAndFlush(new KinematicsResult(
                analyzed, 1, "monopivot-reference-v2", "{\"leverageCurve\":[]}",
                "{\"conditions\":{}}", "{\"antiSquat\":true}"));
        entityManager.clear();

        List<OwnedBikeSummary> summaries = bikeRepository.findSummariesByOwnerId(owner.getId());

        assertThat(summaries)
                .extracting(OwnedBikeSummary::id)
                .containsExactly(analyzed.getId(), draft.getId());
        assertThat(summaries)
                .extracting(OwnedBikeSummary::analyzed)
                .containsExactly(true, false);
    }

    @Test
    void returnsOnlyPendingBikesInPublicationRequestOrder() {
        User firstOwner = userRepository.saveAndFlush(new User(
                "first@example.com", "firstowner", "password-hash", Role.USER));
        User secondOwner = userRepository.saveAndFlush(new User(
                "second@example.com", "secondowner", "password-hash", Role.USER));
        Bike createdFirst = bikeRepository.saveAndFlush(
                Bike.createPrivate(firstOwner, details("Requested second")));
        Bike privateBike = bikeRepository.saveAndFlush(
                Bike.createPrivate(firstOwner, details("Private")));
        Bike createdLast = bikeRepository.saveAndFlush(
                Bike.createPrivate(secondOwner, details("Requested first")));
        Instant earlierRequest = Instant.parse("2026-09-15T18:00:00Z");
        Instant laterRequest = Instant.parse("2026-09-15T18:05:00Z");
        createdLast.requestPublication(earlierRequest);
        createdFirst.requestPublication(laterRequest);
        bikeRepository.saveAllAndFlush(List.of(createdFirst, privateBike, createdLast));
        entityManager.clear();

        var summaries = bikeRepository.findPendingSummaries();

        assertThat(summaries)
                .extracting(summary -> summary.model())
                .containsExactly("Requested first", "Requested second");
        assertThat(summaries)
                .extracting(summary -> summary.ownerUsername())
                .containsExactly("secondowner", "firstowner");
        assertThat(summaries)
                .extracting(summary -> summary.requestedAt())
                .containsExactly(earlierRequest, laterRequest);
    }

    @Test
    void returnsOnlyPublicBikesAndAppliesTheCategoryFilter() {
        User owner = userRepository.saveAndFlush(new User(
                "catalog-owner@example.com", "catalogowner", "password-hash", Role.USER));
        Bike publicEnduro = bikeRepository.saveAndFlush(
                Bike.createPrivate(owner, details("Public enduro")));
        Bike publicDownhill = bikeRepository.saveAndFlush(
                Bike.createPrivate(owner, details("Public downhill", BikeCategory.DOWNHILL)));
        Bike privateBike = bikeRepository.saveAndFlush(
                Bike.createPrivate(owner, details("Private bike")));

        publicEnduro.requestPublication();
        publicEnduro.approvePublication();
        publicDownhill.requestPublication();
        publicDownhill.approvePublication();
        bikeRepository.saveAllAndFlush(List.of(publicEnduro, publicDownhill, privateBike));
        entityManager.clear();

        var allPublic = bikeRepository.findPublicSummaries(
                null, PageRequest.of(0, ListPublicBikesService.PAGE_SIZE));
        var enduroOnly = bikeRepository.findPublicSummaries(
                BikeCategory.ENDURO, PageRequest.of(0, ListPublicBikesService.PAGE_SIZE));

        assertThat(allPublic.getContent())
                .extracting(PublicBikeSummary::model)
                .containsExactly("Public downhill", "Public enduro");
        assertThat(enduroOnly.getContent())
                .extracting(PublicBikeSummary::model)
                .containsExactly("Public enduro");
    }

    private BikeDetails details(String model) {
        return details(model, BikeCategory.ENDURO);
    }

    private BikeDetails details(String model, BikeCategory category) {
        return new BikeDetails(
                "Orange", model, (short) 2020, category,
                SuspensionLayout.SINGLE_PIVOT, 150, 230, 65,
                WheelConfiguration.FULL_29, CassetteType.TWELVE_SPEED,
                (short) 32, (short) 50, 30);
    }

    private MarkedPhotoGeometry geometry() {
        return MarkedPhotoGeometry.create(1800, 1200, List.of(
                new MarkedPhotoPoint(PointType.MAIN_PIVOT, 805, 796),
                new MarkedPhotoPoint(PointType.SHOCK_FRAME, 923, 640),
                new MarkedPhotoPoint(PointType.SHOCK_SWINGARM, 760, 661),
                new MarkedPhotoPoint(PointType.BOTTOM_BRACKET, 778, 855),
                new MarkedPhotoPoint(PointType.REAR_AXLE, 409, 826),
                new MarkedPhotoPoint(PointType.FRONT_AXLE, 1433, 826)));
    }
}
