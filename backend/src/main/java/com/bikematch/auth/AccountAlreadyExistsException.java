package com.bikematch.auth;

public class AccountAlreadyExistsException extends RuntimeException {

    public AccountAlreadyExistsException() {
        super("Email or username is already in use");
    }

    public AccountAlreadyExistsException(Throwable cause) {
        super("Email or username is already in use", cause);
    }
}
