package com.bikematch.bike;

import com.bikematch.kinematics.model.PointType;

import java.util.EnumSet;

public enum SuspensionLayout {
    SINGLE_PIVOT(EnumSet.of(
            PointType.MAIN_PIVOT,
            PointType.SHOCK_FRAME,
            PointType.SHOCK_SWINGARM,
            PointType.BOTTOM_BRACKET,
            PointType.REAR_AXLE,
            PointType.FRONT_AXLE)),
    HORST_LINK(EnumSet.of(
            PointType.MAIN_PIVOT,
            PointType.HORST_PIVOT,
            PointType.ROCKER_FRAME_PIVOT,
            PointType.ROCKER_SEATSTAY_PIVOT,
            PointType.SHOCK_FRAME,
            PointType.SHOCK_ROCKER,
            PointType.BOTTOM_BRACKET,
            PointType.REAR_AXLE,
            PointType.FRONT_AXLE));

    private final EnumSet<PointType> requiredPointTypes;

    SuspensionLayout(EnumSet<PointType> requiredPointTypes) {
        this.requiredPointTypes = requiredPointTypes;
    }

    public EnumSet<PointType> requiredPointTypes() {
        return EnumSet.copyOf(requiredPointTypes);
    }

    public PointType movingShockEye() {
        return this == SINGLE_PIVOT ? PointType.SHOCK_SWINGARM : PointType.SHOCK_ROCKER;
    }
}
