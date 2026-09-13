package com.bikematch.bike;

public class InvalidBikePhotoException extends RuntimeException {

    public InvalidBikePhotoException(String message) {
        super(message);
    }

    public InvalidBikePhotoException(String message, Throwable cause) {
        super(message, cause);
    }
}
