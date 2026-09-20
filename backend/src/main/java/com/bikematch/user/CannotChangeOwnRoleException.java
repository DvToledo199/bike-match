package com.bikematch.user;

public class CannotChangeOwnRoleException extends RuntimeException {
    public CannotChangeOwnRoleException() {
        super("An administrator cannot change their own role");
    }
}
