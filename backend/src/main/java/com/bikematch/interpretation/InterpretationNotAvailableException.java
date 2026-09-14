package com.bikematch.interpretation;

public class InterpretationNotAvailableException extends RuntimeException {

    public InterpretationNotAvailableException() {
        super("Interpretation is not available for this bike");
    }
}
