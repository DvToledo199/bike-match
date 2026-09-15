package com.bikematch.moderation;

import com.bikematch.bike.Bike;
import com.bikematch.user.User;
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

/**
 * Tells an owner that a moderator removed one of their bikes, and why. The bike itself is
 * deleted, so the notice keeps the brand and model its owner needs to recognise it.
 */
@Entity
@Table(name = "bike_removal_notices")
public class BikeRemovalNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    private String brand;

    private String model;

    private String reason;

    @Column(name = "removed_at", updatable = false)
    private Instant removedAt;

    @Column(name = "dismissed_at")
    private Instant dismissedAt;

    protected BikeRemovalNotice() {
    }

    private BikeRemovalNotice(User owner, String brand, String model, String reason) {
        this.owner = owner;
        this.brand = brand;
        this.model = model;
        this.reason = reason;
        this.removedAt = Instant.now();
    }

    public static BikeRemovalNotice forRemovedBike(Bike bike, String reason) {
        Objects.requireNonNull(bike, "Removed bike is required");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A removal reason is required");
        }
        return new BikeRemovalNotice(bike.getOwner(), bike.getBrand(), bike.getModel(), reason.strip());
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

    public String getReason() {
        return reason;
    }

    public Instant getRemovedAt() {
        return removedAt;
    }

    public Instant getDismissedAt() {
        return dismissedAt;
    }

    /** Hides the notice from its owner's list; dismissing it again keeps the first moment. */
    public void dismiss() {
        if (dismissedAt == null) {
            dismissedAt = Instant.now();
        }
    }
}
