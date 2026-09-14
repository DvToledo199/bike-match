package com.bikematch.interpretation;

public interface InterpretationProvider {

    String providerVersion();

    String promptVersion();

    Interpretation generate(InterpretationContext context);
}
