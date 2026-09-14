package com.bikematch.bike;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kinematics_results")
public class KinematicsResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bike_id", nullable = false, unique = true)
    private Bike bike;

    @Column(name = "result_version")
    private int resultVersion;

    @Column(name = "engine_version")
    private String engineVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String curves;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String descriptors;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String capabilities;

    @Column(name = "computed_at", updatable = false)
    private Instant computedAt;

    protected KinematicsResult() {
    }

    public KinematicsResult(Bike bike, int resultVersion, String engineVersion, String curves,
                            String descriptors, String capabilities) {
        this.bike = Objects.requireNonNull(bike, "Bike is required");
        if (resultVersion < 1) {
            throw new IllegalArgumentException("Result version must be positive");
        }
        this.resultVersion = resultVersion;
        this.engineVersion = requireText(engineVersion, "Engine version");
        this.curves = requireText(curves, "Curves");
        this.descriptors = requireText(descriptors, "Descriptors");
        this.capabilities = requireText(capabilities, "Capabilities");
        this.computedAt = Instant.now();
        bike.setKinematicsResult(this);
    }

    public Long getId() {
        return id;
    }

    public Bike getBike() {
        return bike;
    }

    public String getEngineVersion() {
        return engineVersion;
    }

    public int getResultVersion() {
        return resultVersion;
    }

    public String getCurves() {
        return curves;
    }

    public String getDescriptors() {
        return descriptors;
    }

    public String getCapabilities() {
        return capabilities;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }
}
