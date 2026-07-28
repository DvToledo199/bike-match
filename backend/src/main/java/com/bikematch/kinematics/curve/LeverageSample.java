package com.bikematch.kinematics.curve;

/** One point of the leverage curve: the leverage ratio at a given rear-wheel travel (mm). */
public record LeverageSample(double wheelTravelMm, double ratio) {
}