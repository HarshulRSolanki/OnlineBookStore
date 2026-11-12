package com.example.demo.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "delivery_config")
public class DeliveryConfig {

    public static final Long SINGLETON_ID = 1L;
    public static final String DEFAULT_PICKUP_INSTRUCTIONS =
            "Please bring your Order Number and a valid photo ID to collect your books from the selected bookstore counter during opening hours.";

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "home_delivery_charge", nullable = false)
    private Double homeDeliveryCharge = 0.0;

    @Column(name = "store_pickup_charge", nullable = false)
    private Double storePickupCharge = 0.0;

    @Column(name = "pickup_instructions", length = 600)
    private String pickupInstructions = DEFAULT_PICKUP_INSTRUCTIONS;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onSave() {
        if (id == null) {
            id = SINGLETON_ID;
        }
        if (homeDeliveryCharge == null || homeDeliveryCharge < 0) {
            homeDeliveryCharge = 0.0;
        }
        if (storePickupCharge == null || storePickupCharge < 0) {
            storePickupCharge = 0.0;
        }
        if (pickupInstructions == null || pickupInstructions.isBlank()) {
            pickupInstructions = DEFAULT_PICKUP_INSTRUCTIONS;
        }
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getHomeDeliveryCharge() {
        return homeDeliveryCharge != null ? Math.max(0.0, homeDeliveryCharge) : 0.0;
    }

    public void setHomeDeliveryCharge(Double homeDeliveryCharge) {
        this.homeDeliveryCharge = homeDeliveryCharge != null ? Math.max(0.0, homeDeliveryCharge) : 0.0;
    }

    public Double getStorePickupCharge() {
        return storePickupCharge != null ? Math.max(0.0, storePickupCharge) : 0.0;
    }

    public void setStorePickupCharge(Double storePickupCharge) {
        this.storePickupCharge = storePickupCharge != null ? Math.max(0.0, storePickupCharge) : 0.0;
    }

    public String getPickupInstructions() {
        return (pickupInstructions != null && !pickupInstructions.isBlank())
                ? pickupInstructions
                : DEFAULT_PICKUP_INSTRUCTIONS;
    }

    public void setPickupInstructions(String pickupInstructions) {
        this.pickupInstructions = pickupInstructions;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

