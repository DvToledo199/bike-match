package com.bikematch.kinematics;

/** One point of the leverage curve: the leverage ratio at a given rear-wheel travel (mm). */
record LeverageSample(double wheelTravelMm, double ratio) {
}