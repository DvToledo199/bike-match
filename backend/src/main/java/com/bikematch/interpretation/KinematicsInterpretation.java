package com.bikematch.interpretation;

import com.bikematch.bike.KinematicsResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kinematics_interpretations")
public class KinematicsInterpretation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kinematics_result_id", nullable = false)
    private KinematicsResult kinematicsResult;

    @Column(name = "result_version", nullable = false)
    private int resultVersion;

    @Column(name = "interpretation_context_version", nullable = false)
    private int interpretationContextVersion;

    @Column(name = "rules_version", nullable = false, length = 40)
    private String rulesVersion;

    @Column(nullable = false, length = 10)
    private String language;

    @Column(name = "provider_version", nullable = false, length = 80)
    private String providerVersion;

    @Column(name = "prompt_version", nullable = false, length = 40)
    private String promptVersion;

    @Column(nullable = false, length = 20)
    private String source;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String evidence;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private Instant generatedAt;

    protected KinematicsInterpretation() {
    }

    public KinematicsInterpretation(
            KinematicsResult kinematicsResult,
            InterpretationContext context,
            Interpretation interpretation,
            String promptVersion,
            String evidenceJson
    ) {
        this.kinematicsResult = Objects.requireNonNull(kinematicsResult, "Kinematics result is required");
        this.resultVersion = context.resultVersion();
        this.interpretationContextVersion = context.interpretationContextVersion();
        this.rulesVersion = context.rulesVersion();
        this.language = context.language();
        this.providerVersion = interpretation.providerVersion();
        this.promptVersion = requireText(promptVersion, "Prompt version");
        this.source = interpretation.source().name();
        this.summary = requireText(interpretation.summary(), "Summary");
        this.evidence = requireText(evidenceJson, "Evidence");
        this.generatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public KinematicsResult getKinematicsResult() {
        return kinematicsResult;
    }

    public int getResultVersion() {
        return resultVersion;
    }

    public int getInterpretationContextVersion() {
        return interpretationContextVersion;
    }

    public String getRulesVersion() {
        return rulesVersion;
    }

    public String getLanguage() {
        return language;
    }

    public String getProviderVersion() {
        return providerVersion;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getSource() {
        return source;
    }

    public String getSummary() {
        return summary;
    }

    public String getEvidence() {
        return evidence;
    }

    public Instant getGeneratedAt() {
        return generatedAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
