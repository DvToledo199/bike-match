-- A bike is created privately and is progressively completed with a photo and marked points.
-- The calculated result is kept separately because it is derived data, not bike metadata.
CREATE TABLE bikes (
    id                    BIGSERIAL PRIMARY KEY,
    owner_id              BIGINT       NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    brand                 VARCHAR(80)  NOT NULL,
    model                 VARCHAR(100) NOT NULL,
    model_year            SMALLINT,
    category              VARCHAR(30)  NOT NULL,
    suspension_type       VARCHAR(30)  NOT NULL,
    declared_travel_mm    DOUBLE PRECISION NOT NULL,
    shock_eye_to_eye_mm   DOUBLE PRECISION NOT NULL,
    shock_stroke_mm       DOUBLE PRECISION NOT NULL,
    wheel_configuration   VARCHAR(20),
    cassette_type         VARCHAR(20)  NOT NULL,
    chainring_teeth       SMALLINT     NOT NULL,
    sprocket_teeth        SMALLINT     NOT NULL,
    sag_percent           DOUBLE PRECISION NOT NULL DEFAULT 30,
    photo_url             VARCHAR(2048),
    linkage_points        JSONB,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PRIVATE',
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT bikes_brand_not_blank_check
        CHECK (char_length(btrim(brand)) > 0),
    CONSTRAINT bikes_model_not_blank_check
        CHECK (char_length(btrim(model)) > 0),
    CONSTRAINT bikes_category_not_blank_check
        CHECK (char_length(btrim(category)) > 0),
    CONSTRAINT bikes_suspension_type_not_blank_check
        CHECK (char_length(btrim(suspension_type)) > 0),
    CONSTRAINT bikes_model_year_check
        CHECK (model_year IS NULL OR model_year BETWEEN 1900 AND 2100),
    CONSTRAINT bikes_declared_travel_check
        CHECK (declared_travel_mm BETWEEN 50 AND 250),
    CONSTRAINT bikes_shock_eye_to_eye_check
        CHECK (shock_eye_to_eye_mm BETWEEN 100 AND 300),
    CONSTRAINT bikes_shock_stroke_check
        CHECK (shock_stroke_mm BETWEEN 20 AND 120),
    CONSTRAINT bikes_wheel_configuration_check
        CHECK (wheel_configuration IS NULL
            OR wheel_configuration IN ('FULL_29', 'MULLET', 'FULL_27_5')),
    CONSTRAINT bikes_cassette_type_check
        CHECK (cassette_type IN ('TWELVE_SPEED', 'DH_7_8')),
    CONSTRAINT bikes_chainring_teeth_check
        CHECK (chainring_teeth BETWEEN 20 AND 60),
    CONSTRAINT bikes_sprocket_teeth_check
        CHECK (sprocket_teeth BETWEEN 10 AND 60),
    CONSTRAINT bikes_sag_check
        CHECK (sag_percent BETWEEN 10 AND 50),
    CONSTRAINT bikes_photo_url_not_blank_check
        CHECK (photo_url IS NULL OR char_length(btrim(photo_url)) > 0),
    CONSTRAINT bikes_linkage_points_object_check
        CHECK (linkage_points IS NULL OR jsonb_typeof(linkage_points) = 'object'),
    CONSTRAINT bikes_status_check
        CHECK (status IN ('PRIVATE', 'PENDING', 'PUBLIC', 'REJECTED'))
);

CREATE INDEX bikes_owner_id_index ON bikes (owner_id);
CREATE INDEX bikes_public_catalog_index ON bikes (category, created_at DESC)
    WHERE status = 'PUBLIC';

-- One current calculation per bike. The JSONB blocks preserve the complete canonical
-- response defined in docs/contrato-interpretacion-cinematica.md for later graphs and IA.
CREATE TABLE kinematics_results (
    id              BIGSERIAL PRIMARY KEY,
    bike_id         BIGINT       NOT NULL UNIQUE REFERENCES bikes (id) ON DELETE CASCADE,
    result_version  INTEGER      NOT NULL DEFAULT 1,
    engine_version  VARCHAR(40)  NOT NULL,
    curves          JSONB        NOT NULL,
    descriptors     JSONB        NOT NULL,
    capabilities    JSONB        NOT NULL,
    computed_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT kinematics_results_result_version_check
        CHECK (result_version > 0),
    CONSTRAINT kinematics_results_engine_version_check
        CHECK (engine_version IN ('monopivot-v1', 'monopivot-reference-v2')),
    CONSTRAINT kinematics_results_curves_object_check
        CHECK (jsonb_typeof(curves) = 'object'),
    CONSTRAINT kinematics_results_descriptors_object_check
        CHECK (jsonb_typeof(descriptors) = 'object'),
    CONSTRAINT kinematics_results_capabilities_object_check
        CHECK (jsonb_typeof(capabilities) = 'object')
);
