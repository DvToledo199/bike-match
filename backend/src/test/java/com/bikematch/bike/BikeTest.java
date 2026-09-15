package com.bikematch.bike;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.user.Role;
import com.bikematch.user.User;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
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
        bike.attachLinkagePoints(geometry());
        new KinematicsResult(bike, 1, "monopivot-reference-v2", "{}", "{}", "{}");

        assertThatThrownBy(() -> bike.attachPhoto(SECOND_PHOTO))
                .isInstanceOf(BikeAnalysisLockedException.class)
                .hasMessage("An analyzed bike's photo and marked points cannot be changed");
        assertThat(bike.getPhotoUrl()).isEqualTo(FIRST_PHOTO.toString());
        assertThat(bike.isAnalyzed()).isTrue();
    }

    @Test
    void analyzedBikeCannotReplaceItsMarkedPoints() {
        Bike bike = bike();
        bike.attachPhoto(FIRST_PHOTO);
        MarkedPhotoGeometry original = geometry();
        bike.attachLinkagePoints(original);
        new KinematicsResult(bike, 1, "monopivot-reference-v2", "{}", "{}", "{}");
        List<MarkedPhotoPoint> movedPoints = new ArrayList<>(original.points());
        MarkedPhotoPoint pivot = movedPoints.get(0);
        movedPoints.set(0, new MarkedPhotoPoint(pivot.type(), pivot.x() + 1, pivot.y()));

        assertThatThrownBy(() -> bike.attachLinkagePoints(
                MarkedPhotoGeometry.create(1800, 1200, movedPoints)))
                .isInstanceOf(BikeAnalysisLockedException.class);
        assertThat(bike.getLinkagePoints()).isEqualTo(original);
    }

    @Test
    void privateBikeCanRequestPublication() {
        Bike bike = bike();
        assertThat(bike.getPublicationRequestedAt()).isNull();

        bike.requestPublication();

        assertThat(bike.getStatus()).isEqualTo(BikeStatus.PENDING);
        assertThat(bike.getPublicationRequestedAt()).isNotNull();
    }

    @Test
    void pendingBikeCanBeApprovedOrRejected() {
        Bike approvedBike = bike();
        approvedBike.requestPublication();
        Bike rejectedBike = bike();
        rejectedBike.requestPublication();

        approvedBike.approvePublication();
        rejectedBike.rejectPublication();

        assertThat(approvedBike.getStatus()).isEqualTo(BikeStatus.PUBLIC);
        assertThat(rejectedBike.getStatus()).isEqualTo(BikeStatus.REJECTED);
    }

    @Test
    void nonPendingBikeCannotReceiveAModerationDecision() {
        Bike bike = bike();

        assertThatThrownBy(bike::approvePublication)
                .isInstanceOf(BikeNotPendingException.class)
                .hasMessage("Only pending bikes can be approved or rejected");
        assertThat(bike.getStatus()).isEqualTo(BikeStatus.PRIVATE);
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
