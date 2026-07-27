package com.bikematch.kinematics;

/**
 * A real bike with known reference curves, used to validate the engine.
 * Each implementation holds the points marked on its photo (in pixels),
 * calibrates them to millimetres and hands the engine a ready KinematicsInput.
 */
interface ReferenceBike {

    KinematicsInput input();

    double declaredTravelMm();
}
