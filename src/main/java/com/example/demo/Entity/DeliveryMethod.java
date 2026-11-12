package com.example.demo.Entity;

public enum DeliveryMethod {
    HOME_DELIVERY("Home Delivery", "Online Delivery to Your Address"),
    STORE_PICKUP("Physical Store Pickup", "Collect from Physical Bookstore");

    private final String displayName;
    private final String description;

    DeliveryMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public static DeliveryMethod fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (DeliveryMethod method : values()) {
            if (method.name().equals(normalized)) {
                return method;
            }
        }
        return null;
    }
}

