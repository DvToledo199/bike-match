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
            PointType.FRONT_AXLE)),
    HORST_LINK_YOKE(EnumSet.of(
            PointType.MAIN_PIVOT,
            PointType.HORST_PIVOT,
            PointType.ROCKER_FRAME_PIVOT,
            PointType.ROCKER_SEATSTAY_PIVOT,
            PointType.SHOCK_FRAME,
            PointType.YOKE_ROCKER_PIVOT,
            PointType.SHOCK_YOKE_EYE,
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
        return switch (this) {
            case SINGLE_PIVOT -> PointType.SHOCK_SWINGARM;
            case HORST_LINK -> PointType.SHOCK_ROCKER;
            case HORST_LINK_YOKE -> PointType.SHOCK_YOKE_EYE;
        };
    }

    public boolean isHorstLink() {
        return this == HORST_LINK || this == HORST_LINK_YOKE;
    }
}
