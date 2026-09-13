package com.bikematch.bike;

public class BikeNotFoundException extends RuntimeException {

    public BikeNotFoundException() {
        super("Bike not found");
    }
}
