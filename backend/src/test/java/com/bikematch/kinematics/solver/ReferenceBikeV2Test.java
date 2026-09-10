package com.bikematch.kinematics.solver;

import com.bikematch.kinematics.api.KinematicsParametersDto;
import com.bikematch.kinematics.api.KinematicsService;
import com.bikematch.kinematics.api.PointDto;
import com.bikematch.kinematics.api.PreviewRequest;
import com.bikematch.kinematics.model.PointType;
import com.bikematch.kinematics.model.WheelConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Reuse the original real-bike fixtures; do not widen the existing leverage/travel tolerances. */
class ReferenceBikeV2Test {
    @Test
    void stage6RetainsLeverageAndTravelAgreement() {
        assertReference(new OrangeStage6Fixture(), WheelConfiguration.FULL_29, 2.775, 2.675);
    }

    @Test
    void surgeRetainsLeverageAndTravelAgreement() {
        assertReference(new OrangeSurgeFixture(), WheelConfiguration.FULL_27_5, 2.55, 2.50);
    }

    private void assertReference(ReferenceBike bike, WheelConfiguration wheels, double initialLr, double finalLr) {
        var input = bike.input();
        var p = input.parameters();
        var points = input.points().stream().map(point -> new PointDto(point.type(),
                point.position().x(), point.position().y())).toList();
        double eyeToEye = input.pointOf(PointType.SHOCK_FRAME).distanceTo(input.pointOf(PointType.SHOCK_SWINGARM));
        var request = new PreviewRequest(points, eyeToEye, new KinematicsParametersDto(p.shockStrokeMm(),
                p.chainringTeeth(), p.sprocketTeeth(), p.declaredTravelMm(), p.sagPercent(), wheels));
        var result = new KinematicsService().preview(request);
        assertEquals(bike.declaredTravelMm(), result.travelCheck().calculatedTravelMm(), bike.declaredTravelMm() * 0.03);
        assertEquals(initialLr, result.leverageDescriptors().lrInitial(), initialLr * 0.03);
        assertEquals(finalLr, result.leverageDescriptors().lrFinal(), finalLr * 0.03);
    }
}
