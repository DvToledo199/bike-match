package com.bikematch.bike;

/** Where a bike's photo lives in image storage: one replaceable asset per bike. */
final class BikePhotos {

    private BikePhotos() {
    }

    static String publicId(long bikeId) {
        return "bikematch/bikes/" + bikeId;
    }
}
