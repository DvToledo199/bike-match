package com.bikematch.interpretation;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KinematicsInterpretationRepository
        extends JpaRepository<KinematicsInterpretation, Long> {

    Optional<KinematicsInterpretation> findByKinematicsResultIdAndResultVersionAndInterpretationContextVersionAndRulesVersionAndLanguageAndProviderVersionAndPromptVersion(
            Long kinematicsResultId,
            int resultVersion,
            int interpretationContextVersion,
            String rulesVersion,
            String language,
            String providerVersion,
            String promptVersion
    );
}
