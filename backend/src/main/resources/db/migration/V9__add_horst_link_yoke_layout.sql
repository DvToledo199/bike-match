ALTER TABLE bikes
    DROP CONSTRAINT bikes_suspension_layout_values_check;

ALTER TABLE bikes
    ADD CONSTRAINT bikes_suspension_layout_values_check
        CHECK (suspension_layout IN ('SINGLE_PIVOT', 'HORST_LINK', 'HORST_LINK_YOKE'));

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
            'horst-link-yoke-reference-v1'
        ));
