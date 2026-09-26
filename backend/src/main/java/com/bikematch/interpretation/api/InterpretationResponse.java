package com.bikematch.interpretation.api;

import com.bikematch.interpretation.InterpretationService.InterpretationView;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record InterpretationResponse(
        int resultVersion,
        int interpretationContextVersion,
        String rulesVersion,
        String language,
        String source,
        String providerVersion,
        String summary,
        JsonNode evidence,
        Instant generatedAt,
        boolean fallback
) {

    public static InterpretationResponse from(InterpretationView view) {
        return new InterpretationResponse(
                view.resultVersion(),
                view.interpretationContextVersion(),
                view.rulesVersion(),
                view.language(),
                view.source(),
                view.providerVersion(),
                view.summary(),
                view.evidence(),
                view.generatedAt(),
                view.fallback());
    }
}
