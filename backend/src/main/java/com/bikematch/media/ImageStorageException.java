package com.bikematch.media;

public class ImageStorageException extends RuntimeException {

    public ImageStorageException(Throwable cause) {
        super("Photo storage is temporarily unavailable", cause);
    }

    public ImageStorageException() {
        super("Photo storage returned an invalid response");
    }
}
