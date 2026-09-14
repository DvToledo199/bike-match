package com.bikematch.bike;

public class BikeNotPendingException extends RuntimeException {

    public BikeNotPendingException() {
        super("Only pending bikes can be approved or rejected");
    }
}
