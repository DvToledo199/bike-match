package com.bikematch.bike;

public class BikeAnalysisLockedException extends RuntimeException {

    public BikeAnalysisLockedException() {
        super("An analyzed bike's photo and marked points cannot be changed");
    }
}
