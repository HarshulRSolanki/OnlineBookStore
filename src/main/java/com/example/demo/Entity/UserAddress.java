package com.example.demo.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "user_addresses")
public class UserAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "house_no", nullable = false, length = 100)
    private String houseNo;

    @Column(name = "building_name", nullable = false, length = 140)
    private String buildingName;

    @Column(name = "street_area", nullable = false, length = 180)
    private String streetArea;

    @Column(name = "landmark", length = 180)
    private String landmark;

    @Column(name = "country", nullable = false, length = 60)
    private String country = "India";

    @Column(name = "state", nullable = false, length = 80)
    private String state;

    @Column(name = "city", nullable = false, length = 80)
    private String city;

    @Column(name = "pin_code", nullable = false, length = 10)
    private String pinCode;

    @Column(name = "address_type", nullable = false, length = 20)
    private String addressType = "HOME"; // HOME, OFFICE, OTHER

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (country == null || country.isBlank()) {
            country = "India";
        }
        if (addressType == null || addressType.isBlank()) {
            addressType = "HOME";
        } else {
            addressType = addressType.trim().toUpperCase();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (country == null || country.isBlank()) {
            country = "India";
        }
        if (addressType != null) {
            addressType = addressType.trim().toUpperCase();
        }
    }

    @Transient
    public boolean isComplete() {
        return isNotBlank(fullName)
                && mobileNumber != null && mobileNumber.trim().matches("^[6-9][0-9]{9}$")
                && isNotBlank(houseNo)
                && isNotBlank(buildingName)
                && isNotBlank(streetArea)
                && "India".equalsIgnoreCase(country != null ? country.trim() : "")
                && isNotBlank(state)
                && isNotBlank(city)
                && pinCode != null && pinCode.trim().matches("^[1-9][0-9]{5}$")
                && isNotBlank(addressType);
    }

    @Transient
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (isNotBlank(houseNo)) {
            sb.append(houseNo.trim());
        }
        if (isNotBlank(buildingName)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(buildingName.trim());
        }
        if (isNotBlank(streetArea)) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(streetArea.trim());
        }
        if (isNotBlank(landmark)) {
            sb.append(" (Near ").append(landmark.trim()).append(")");
        }
        if (isNotBlank(city)) {
            sb.append(", ").append(city.trim());
        }
        if (isNotBlank(state)) {
            sb.append(", ").append(state.trim());
        }
        if (isNotBlank(pinCode)) {
            sb.append(" - ").append(pinCode.trim());
        }
        sb.append(", ").append(isNotBlank(country) ? country.trim() : "India");
        return sb.toString();
    }

    private boolean isNotBlank(String val) {
        return val != null && !val.trim().isEmpty();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber = mobileNumber;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getStreetArea() {
        return streetArea;
    }

    public void setStreetArea(String streetArea) {
        this.streetArea = streetArea;
    }

    public String getLandmark() {
        return landmark;
    }

    public void setLandmark(String landmark) {
        this.landmark = landmark;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPinCode() {
        return pinCode;
    }

    public void setPinCode(String pinCode) {
        this.pinCode = pinCode;
    }

    public String getAddressType() {
        return addressType;
    }

    public void setAddressType(String addressType) {
        this.addressType = addressType;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

