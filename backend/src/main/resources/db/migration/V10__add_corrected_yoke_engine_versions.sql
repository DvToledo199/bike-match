ALTER TABLE kinematics_results
    DROP CONSTRAINT kinematics_results_engine_version_check;

ALTER TABLE kinematics_results
    ADD CONSTRAINT kinematics_results_engine_version_check
        CHECK (engine_version IN (
            'monopivot-v1',
            'monopivot-reference-v2',
            'horst-link-v1',
            'horst-link-reference-v1',
            'horst-link-yoke-v1',
            'horst-link-yoke-reference-v1',
            'horst-link-yoke-v2',
            'horst-link-yoke-reference-v2'
        ));
