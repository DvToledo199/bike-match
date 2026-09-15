package com.bikematch.media;

public class ImageStorageException extends RuntimeException {

    public ImageStorageException(Throwable cause) {
        super("The photo could not be stored", cause);
    }

    public ImageStorageException() {
        super("Photo storage returned an invalid response");
    }
}
