package com.bikematch.interpretation;

public class InterpretationRateLimitException extends RuntimeException {

    public InterpretationRateLimitException() {
        super("Please wait before requesting another interpretation");
    }
}
