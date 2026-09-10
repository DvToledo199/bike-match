package com.bikematch.kinematics.model;

/** Reference outer radii: 622/584 mm bead seats plus a nominal 60 mm tyre height. */
public enum WheelConfiguration {
    FULL_29(371, 371),
    MULLET(371, 352),
    FULL_27_5(352, 352);

    private final double frontRadiusMm;
    private final double rearRadiusMm;

    WheelConfiguration(double frontRadiusMm, double rearRadiusMm) {
        this.frontRadiusMm = frontRadiusMm;
        this.rearRadiusMm = rearRadiusMm;
    }

    public double frontRadiusMm() { return frontRadiusMm; }
    public double rearRadiusMm() { return rearRadiusMm; }
}
