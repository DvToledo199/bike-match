package com.bikematch.bike;

import com.bikematch.kinematics.model.WheelConfiguration;
import com.bikematch.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.net.URI;
import java.time.Instant;
import java.util.Objects;
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

    @Enumerated(EnumType.STRING)
    private BikeCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "suspension_layout")
    private SuspensionLayout suspensionLayout;

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

    private Bike(User owner, BikeDetails details) {
        this.owner = Objects.requireNonNull(owner, "Owner is required");
        this.brand = details.brand();
        this.model = details.model();
        this.modelYear = details.modelYear();
        this.category = details.category();
        this.suspensionLayout = details.suspensionLayout();
        this.declaredTravelMm = details.declaredTravelMm();
        this.shockEyeToEyeMm = details.shockEyeToEyeMm();
        this.shockStrokeMm = details.shockStrokeMm();
        this.wheelConfiguration = details.wheelConfiguration();
        this.cassetteType = details.cassetteType();
        this.chainringTeeth = details.chainringTeeth();
        this.sprocketTeeth = details.sprocketTeeth();
        this.sagPercent = details.sagPercent();
        this.status = BikeStatus.PRIVATE;
        this.createdAt = Instant.now();
    }

    public static Bike createPrivate(User owner, BikeDetails details) {
        return new Bike(owner, Objects.requireNonNull(details, "Bike details are required"));
    }

    public Long getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    public Short getModelYear() {
        return modelYear;
    }

    public BikeCategory getCategory() {
        return category;
    }

    public SuspensionLayout getSuspensionLayout() {
        return suspensionLayout;
    }

    public BikeStatus getStatus() {
        return status;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void attachPhoto(URI photoUri) {
        Objects.requireNonNull(photoUri, "Photo URI is required");
        String value = photoUri.toString();
        if (!"https".equalsIgnoreCase(photoUri.getScheme())
                || photoUri.getHost() == null
                || value.length() > 2048) {
            throw new IllegalArgumentException("Photo URI must be a valid HTTPS address");
        }
        this.photoUrl = value;
    }

    public KinematicsResult getKinematicsResult() {
        return kinematicsResult;
    }

    void setKinematicsResult(KinematicsResult kinematicsResult) {
        this.kinematicsResult = kinematicsResult;
    }
}
