CREATE TABLE kinematics_interpretations (
    id                            BIGSERIAL PRIMARY KEY,
    kinematics_result_id          BIGINT       NOT NULL REFERENCES kinematics_results (id) ON DELETE CASCADE,
    result_version                INTEGER      NOT NULL,
    interpretation_context_version INTEGER      NOT NULL,
    rules_version                 VARCHAR(40)  NOT NULL,
    language                      VARCHAR(10)  NOT NULL,
    provider_version              VARCHAR(80)  NOT NULL,
    prompt_version                VARCHAR(40)  NOT NULL,
    source                        VARCHAR(20)  NOT NULL,
    summary                       TEXT         NOT NULL,
    evidence                      JSONB        NOT NULL,
    generated_at                  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT kinematics_interpretations_version_check
        CHECK (result_version > 0 AND interpretation_context_version > 0),
    CONSTRAINT kinematics_interpretations_source_check
        CHECK (source IN ('RULES', 'AI')),
    CONSTRAINT kinematics_interpretations_summary_not_blank_check
        CHECK (char_length(btrim(summary)) > 0),
    CONSTRAINT kinematics_interpretations_evidence_array_check
        CHECK (jsonb_typeof(evidence) = 'array'),
    CONSTRAINT kinematics_interpretations_unique_context_check
        UNIQUE (
            kinematics_result_id,
            result_version,
            interpretation_context_version,
            rules_version,
            language,
            provider_version,
            prompt_version
        )
);

CREATE INDEX kinematics_interpretations_result_index
    ON kinematics_interpretations (kinematics_result_id, language);
