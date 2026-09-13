package com.bikematch.bike;

import com.bikematch.user.User;
import com.bikematch.kinematics.model.WheelConfiguration;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "bikes")
public class Bike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String brand;

    private String model;

    @Column(name = "model_year")
    private Short modelYear;

    private String category;

    @Column(name = "suspension_type")
    private String suspensionType;

    @Column(name = "declared_travel_mm")
    private double declaredTravelMm;

    @Column(name = "shock_eye_to_eye_mm")
    private double shockEyeToEyeMm;

    @Column(name = "shock_stroke_mm")
    private double shockStrokeMm;

    @Enumerated(EnumType.STRING)
    @Column(name = "wheel_configuration")
    private WheelConfiguration wheelConfiguration;

    @Enumerated(EnumType.STRING)
    @Column(name = "cassette_type")
    private CassetteType cassetteType;

    @Column(name = "chainring_teeth")
    private short chainringTeeth;

    @Column(name = "sprocket_teeth")
    private short sprocketTeeth;

    @Column(name = "sag_percent")
    private double sagPercent;

    @Column(name = "photo_url")
    private String photoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "linkage_points", columnDefinition = "jsonb")
    private String linkagePoints;

    @Enumerated(EnumType.STRING)
    private BikeStatus status;

    @OneToOne(mappedBy = "bike", fetch = FetchType.LAZY)
    private KinematicsResult kinematicsResult;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Bike() {
    }

    public Bike(User owner, String brand, String model, Short modelYear, String category,
                String suspensionType, double declaredTravelMm, double shockEyeToEyeMm,
                double shockStrokeMm, WheelConfiguration wheelConfiguration, CassetteType cassetteType,
                short chainringTeeth, short sprocketTeeth, double sagPercent) {
        this.owner = owner;
        this.brand = brand;
        this.model = model;
        this.modelYear = modelYear;
        this.category = category;
        this.suspensionType = suspensionType;
        this.declaredTravelMm = declaredTravelMm;
        this.shockEyeToEyeMm = shockEyeToEyeMm;
        this.shockStrokeMm = shockStrokeMm;
        this.wheelConfiguration = wheelConfiguration;
        this.cassetteType = cassetteType;
        this.chainringTeeth = chainringTeeth;
        this.sprocketTeeth = sprocketTeeth;
        this.sagPercent = sagPercent;
        this.status = BikeStatus.PRIVATE;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public BikeStatus getStatus() {
        return status;
    }

    public KinematicsResult getKinematicsResult() {
        return kinematicsResult;
    }

    void setKinematicsResult(KinematicsResult kinematicsResult) {
        this.kinematicsResult = kinematicsResult;
    }
}
