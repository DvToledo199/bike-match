package com.bikematch.kinematics.descriptor;

/** How the leverage ratio behaves over a segment (third) of the travel. */
public enum SegmentTrend {
    PROGRESSIVE,   // LR drops: the bike hardens
    LINEAR,        // LR barely changes
    REGRESSIVE     // LR rises: the bike softens
}
