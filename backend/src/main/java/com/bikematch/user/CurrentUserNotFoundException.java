package com.bikematch.user;

public class CurrentUserNotFoundException extends RuntimeException {

    public CurrentUserNotFoundException() {
        super("Authenticated user no longer exists");
    }
}
