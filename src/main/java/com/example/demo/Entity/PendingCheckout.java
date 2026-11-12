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
import jakarta.persistence.Table;

/**
 * Holds a validated pre-payment snapshot of delivery selection and shipping/pickup
 * details while Stripe Checkout completes. Does NOT create premature Order records
 * if payment fails or is cancelled.
 */
@Entity
@Table(name = "pending_checkouts")
public class PendingCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "checkout_token", nullable = false, unique = true, length = 64)
    private String checkoutToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "checkout_type", nullable = false, length = 20)
    private String checkoutType; // CART or SINGLE

    @Column(name = "single_book_id")
    private Long singleBookId;

    @Column(name = "quantity")
    private Integer quantity = 1;

    @Column(name = "delivery_method", nullable = false, length = 30)
    private String deliveryMethod; // HOME_DELIVERY or STORE_PICKUP

    @Column(name = "delivery_charge", nullable = false)
    private Double deliveryCharge = 0.0;

    // Shipping address snapshot (for HOME_DELIVERY)
    @Column(name = "delivery_address_id")
    private Long deliveryAddressId;

    @Column(name = "shipping_full_name", length = 120)
    private String shippingFullName;

    @Column(name = "shipping_mobile", length = 20)
    private String shippingMobile;

    @Column(name = "shipping_house_no", length = 120)
    private String shippingHouseNo;

    @Column(name = "shipping_building_name", length = 140)
    private String shippingBuildingName;

    @Column(name = "shipping_street_area", length = 180)
    private String shippingStreetArea;

    @Column(name = "shipping_landmark", length = 180)
    private String shippingLandmark;

    @Column(name = "shipping_country", length = 60)
    private String shippingCountry;

    @Column(name = "shipping_state", length = 80)
    private String shippingState;

    @Column(name = "shipping_city", length = 80)
    private String shippingCity;

    @Column(name = "shipping_pin_code", length = 15)
    private String shippingPinCode;

    @Column(name = "shipping_address_type", length = 20)
    private String shippingAddressType;

    @Column(name = "shipping_address_snapshot", length = 1000)
    private String shippingAddressSnapshot;

    // Pickup store snapshot (for STORE_PICKUP)
    @Column(name = "pickup_store_id")
    private Long pickupStoreId;

    @Column(name = "pickup_store_name", length = 150)
    private String pickupStoreName;

    @Column(name = "pickup_store_contact", length = 30)
    private String pickupStoreContact;

    @Column(name = "pickup_store_address", length = 500)
    private String pickupStoreAddress;

    @Column(name = "pickup_store_city", length = 80)
    private String pickupStoreCity;

    @Column(name = "pickup_store_state", length = 80)
    private String pickupStoreState;

    @Column(name = "pickup_store_pin_code", length = 15)
    private String pickupStorePinCode;

    @Column(name = "pickup_store_hours", length = 140)
    private String pickupStoreOpeningHours;

    @Column(name = "pickup_instructions", length = 600)
    private String pickupInstructions;

    @Column(name = "completed", nullable = false)
    private boolean completed = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (deliveryCharge == null) {
            deliveryCharge = 0.0;
        }
        if (quantity == null || quantity <= 0) {
            quantity = 1;
        }
    }

    public void copyFromAddress(UserAddress address) {
        if (address == null) {
            return;
        }
        this.deliveryAddressId = address.getId();
        this.shippingFullName = address.getFullName();
        this.shippingMobile = address.getMobileNumber();
        this.shippingHouseNo = address.getHouseNo();
        this.shippingBuildingName = address.getBuildingName();
        this.shippingStreetArea = address.getStreetArea();
        this.shippingLandmark = address.getLandmark();
        this.shippingCountry = address.getCountry();
        this.shippingState = address.getState();
        this.shippingCity = address.getCity();
        this.shippingPinCode = address.getPinCode();
        this.shippingAddressType = address.getAddressType();
        this.shippingAddressSnapshot = address.getFormattedAddress();
    }

    public void copyFromStore(StoreLocation store, String instructions) {
        if (store == null) {
            return;
        }
        this.pickupStoreId = store.getId();
        this.pickupStoreName = store.getStoreName();
        this.pickupStoreContact = store.getContactNumber();
        this.pickupStoreAddress = store.getAddress();
        this.pickupStoreCity = store.getCity();
        this.pickupStoreState = store.getState();
        this.pickupStorePinCode = store.getPinCode();
        this.pickupStoreOpeningHours = store.getOpeningHours();
        this.pickupInstructions = instructions;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCheckoutToken() {
        return checkoutToken;
    }

    public void setCheckoutToken(String checkoutToken) {
        this.checkoutToken = checkoutToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getCheckoutType() {
        return checkoutType;
    }

    public void setCheckoutType(String checkoutType) {
        this.checkoutType = checkoutType;
    }

    public Long getSingleBookId() {
        return singleBookId;
    }

    public void setSingleBookId(Long singleBookId) {
        this.singleBookId = singleBookId;
    }

    public Integer getQuantity() {
        return quantity != null && quantity > 0 ? quantity : 1;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
    }

    public Double getDeliveryCharge() {
        return deliveryCharge != null ? deliveryCharge : 0.0;
    }

    public void setDeliveryCharge(Double deliveryCharge) {
        this.deliveryCharge = deliveryCharge != null ? deliveryCharge : 0.0;
    }

    public Long getDeliveryAddressId() {
        return deliveryAddressId;
    }

    public void setDeliveryAddressId(Long deliveryAddressId) {
        this.deliveryAddressId = deliveryAddressId;
    }

    public String getShippingFullName() {
        return shippingFullName;
    }

    public void setShippingFullName(String shippingFullName) {
        this.shippingFullName = shippingFullName;
    }

    public String getShippingMobile() {
        return shippingMobile;
    }

    public void setShippingMobile(String shippingMobile) {
        this.shippingMobile = shippingMobile;
    }

    public String getShippingHouseNo() {
        return shippingHouseNo;
    }

    public void setShippingHouseNo(String shippingHouseNo) {
        this.shippingHouseNo = shippingHouseNo;
    }

    public String getShippingBuildingName() {
        return shippingBuildingName;
    }

    public void setShippingBuildingName(String shippingBuildingName) {
        this.shippingBuildingName = shippingBuildingName;
    }

    public String getShippingStreetArea() {
        return shippingStreetArea;
    }

    public void setShippingStreetArea(String shippingStreetArea) {
        this.shippingStreetArea = shippingStreetArea;
    }

    public String getShippingLandmark() {
        return shippingLandmark;
    }

    public void setShippingLandmark(String shippingLandmark) {
        this.shippingLandmark = shippingLandmark;
    }

    public String getShippingCountry() {
        return shippingCountry;
    }

    public void setShippingCountry(String shippingCountry) {
        this.shippingCountry = shippingCountry;
    }

    public String getShippingState() {
        return shippingState;
    }

    public void setShippingState(String shippingState) {
        this.shippingState = shippingState;
    }

    public String getShippingCity() {
        return shippingCity;
    }

    public void setShippingCity(String shippingCity) {
        this.shippingCity = shippingCity;
    }

    public String getShippingPinCode() {
        return shippingPinCode;
    }

    public void setShippingPinCode(String shippingPinCode) {
        this.shippingPinCode = shippingPinCode;
    }

    public String getShippingAddressType() {
        return shippingAddressType;
    }

    public void setShippingAddressType(String shippingAddressType) {
        this.shippingAddressType = shippingAddressType;
    }

    public String getShippingAddressSnapshot() {
        return shippingAddressSnapshot;
    }

    public void setShippingAddressSnapshot(String shippingAddressSnapshot) {
        this.shippingAddressSnapshot = shippingAddressSnapshot;
    }

    public Long getPickupStoreId() {
        return pickupStoreId;
    }

    public void setPickupStoreId(Long pickupStoreId) {
        this.pickupStoreId = pickupStoreId;
    }

    public String getPickupStoreName() {
        return pickupStoreName;
    }

    public void setPickupStoreName(String pickupStoreName) {
        this.pickupStoreName = pickupStoreName;
    }

    public String getPickupStoreContact() {
        return pickupStoreContact;
    }

    public void setPickupStoreContact(String pickupStoreContact) {
        this.pickupStoreContact = pickupStoreContact;
    }

    public String getPickupStoreAddress() {
        return pickupStoreAddress;
    }

    public void setPickupStoreAddress(String pickupStoreAddress) {
        this.pickupStoreAddress = pickupStoreAddress;
    }

    public String getPickupStoreCity() {
        return pickupStoreCity;
    }

    public void setPickupStoreCity(String pickupStoreCity) {
        this.pickupStoreCity = pickupStoreCity;
    }

    public String getPickupStoreState() {
        return pickupStoreState;
    }

    public void setPickupStoreState(String pickupStoreState) {
        this.pickupStoreState = pickupStoreState;
    }

    public String getPickupStorePinCode() {
        return pickupStorePinCode;
    }

    public void setPickupStorePinCode(String pickupStorePinCode) {
        this.pickupStorePinCode = pickupStorePinCode;
    }

    public String getPickupStoreOpeningHours() {
        return pickupStoreOpeningHours;
    }

    public void setPickupStoreOpeningHours(String pickupStoreOpeningHours) {
        this.pickupStoreOpeningHours = pickupStoreOpeningHours;
    }

    public String getPickupInstructions() {
        return pickupInstructions;
    }

    public void setPickupInstructions(String pickupInstructions) {
        this.pickupInstructions = pickupInstructions;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

