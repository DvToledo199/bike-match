package com.bikematch.bike;

public class BikeAccessDeniedException extends RuntimeException {

    public BikeAccessDeniedException() {
        super("You do not have permission to change this bike");
    }
}
