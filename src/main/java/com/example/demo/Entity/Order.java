package com.example.demo.Entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;

@Entity
@AllArgsConstructor
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = true)
    private Book book;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "receipt_number")
    private String receiptNumber;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    private int quantity = 1;
    private double unitPrice;
    private double subtotal;
    private double tax = 0.0;
    private double discount = 0.0;

    @Column(name = "delivery_charge")
    private Double deliveryCharge = 0.0;

    private double amount;

    private String paymentStatus; // PAID, SUCCESS, FAILED, PENDING, CANCELLED

    @Column(name = "order_status", length = 40)
    private String orderStatus = "CONFIRMED"; // ORDER_PLACED, CONFIRMED, PROCESSING, SHIPPED, OUT_FOR_DELIVERY, DELIVERED, READY_FOR_PICKUP, COLLECTED, COMPLETED, CANCELLED

    @Column(name = "delivery_method", length = 30)
    private String deliveryMethod; // HOME_DELIVERY or STORE_PICKUP

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

    private LocalDateTime orderDate;
    private boolean checkedout;

    private boolean receiptSent = false;

    private long ordersCount;
    private long downloadsCount;

    public Order() {
        this.orderDate = LocalDateTime.now();
        this.quantity = 1;
        this.deliveryCharge = 0.0;
        this.orderStatus = "CONFIRMED";
    }

    @PrePersist
    public void prePersist() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd").format(orderDate);
        if (orderNumber == null || orderNumber.trim().isEmpty()) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            orderNumber = "ORD-" + datePart + "-" + suffix;
        }
        if (receiptNumber == null || receiptNumber.trim().isEmpty()) {
            String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
            receiptNumber = "REC-" + datePart + "-" + suffix;
        }
        if (items != null && !items.isEmpty()) {
            int totalQty = 0;
            double itemsSubtotal = 0.0;
            for (OrderItem item : items) {
                totalQty += item.getQuantity();
                itemsSubtotal += item.getSubtotal();
            }
            if (totalQty > 0) {
                this.quantity = totalQty;
            }
            if (this.subtotal <= 0) {
                this.subtotal = itemsSubtotal;
            }
            if (items.size() == 1 && this.unitPrice <= 0) {
                this.unitPrice = items.get(0).getUnitPrice();
            }
        } else {
            if (quantity <= 0) {
                quantity = 1;
            }
            if (unitPrice <= 0 && book != null) {
                unitPrice = book.getPrice();
            }
            if (subtotal <= 0) {
                subtotal = unitPrice * quantity;
            }
        }
        double delivery = deliveryCharge != null ? deliveryCharge : 0.0;
        if (amount <= 0) {
            amount = Math.max(0.0, subtotal + tax + delivery - discount);
        }
        if (orderStatus == null || orderStatus.trim().isEmpty()) {
            orderStatus = isPaid() ? "CONFIRMED" : "PENDING";
        }
    }

    public void addItem(OrderItem item) {
        if (item == null) {
            return;
        }
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(item);
        item.setOrder(this);
    }

    /**
     * Unified item view supporting both multi-book OrderItems and legacy single-book Order rows.
     */
    @Transient
    public List<OrderItem> getEffectiveItems() {
        if (items != null && !items.isEmpty()) {
            return items;
        }
        List<OrderItem> fallback = new ArrayList<>();
        if (book != null) {
            int qty = quantity > 0 ? quantity : 1;
            double up = unitPrice > 0 ? unitPrice : (double) book.getPrice();
            OrderItem legacyItem = new OrderItem(book, qty, up);
            legacyItem.setSubtotal(subtotal > 0 ? subtotal : (up * qty));
            fallback.add(legacyItem);
        }
        return fallback;
    }

    @Transient
    public int getTotalItemQuantity() {
        List<OrderItem> eff = getEffectiveItems();
        if (!eff.isEmpty()) {
            int sum = 0;
            for (OrderItem item : eff) {
                sum += item.getQuantity();
            }
            return sum;
        }
        return getQuantity();
    }

    @Transient
    public int getDistinctBookCount() {
        return getEffectiveItems().size();
    }

    @Transient
    public String getSummaryTitle() {
        List<OrderItem> eff = getEffectiveItems();
        if (eff.isEmpty()) {
            return book != null ? book.getTitle() : "Curated Literary Edition";
        }
        if (eff.size() == 1) {
            return eff.get(0).getEffectiveTitle();
        }
        return eff.get(0).getEffectiveTitle() + " (+" + (eff.size() - 1) + " more)";
    }

    @Transient
    public boolean isPaid() {
        if (paymentStatus == null) {
            return false;
        }
        String upper = paymentStatus.trim().toUpperCase();
        return "PAID".equals(upper) || "SUCCESS".equals(upper);
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

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
        for (OrderItem item : this.items) {
            item.setOrder(this);
        }
    }

    public String getOrderNumber() {
        if (orderNumber == null && id != null) {
            return "ORD-" + id;
        }
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getReceiptNumber() {
        if (receiptNumber == null && id != null) {
            return "REC-" + id;
        }
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public void setStripePaymentIntentId(String stripePaymentIntentId) {
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    public int getQuantity() {
        return quantity <= 0 ? 1 : quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        if (unitPrice <= 0 && book != null) {
            return book.getPrice();
        }
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getSubtotal() {
        if (subtotal <= 0) {
            List<OrderItem> eff = getEffectiveItems();
            if (!eff.isEmpty()) {
                double sum = 0.0;
                for (OrderItem item : eff) {
                    sum += item.getSubtotal();
                }
                return sum;
            }
            return getUnitPrice() * getQuantity();
        }
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTax() {
        return tax;
    }

    public void setTax(double tax) {
        this.tax = tax;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getDeliveryCharge() {
        return deliveryCharge != null ? deliveryCharge : 0.0;
    }

    public void setDeliveryCharge(Double deliveryCharge) {
        this.deliveryCharge = deliveryCharge != null ? deliveryCharge : 0.0;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getOrderStatus() {
        if (orderStatus == null || orderStatus.trim().isEmpty()) {
            return isPaid() ? "CONFIRMED" : "PENDING";
        }
        return orderStatus;
    }

    public void setOrderStatus(String orderStatus) {
        this.orderStatus = orderStatus;
    }

    public LocalDateTime getOrderDate() {
        return orderDate;
    }

    public void setOrderDate(LocalDateTime orderDate) {
        this.orderDate = orderDate;
    }

    public boolean isCheckedout() {
        return checkedout;
    }

    public void setCheckedout(boolean checkedout) {
        this.checkedout = checkedout;
    }

    public boolean isReceiptSent() {
        return receiptSent;
    }

    public void setReceiptSent(boolean receiptSent) {
        this.receiptSent = receiptSent;
    }

    public long getOrdersCount() {
        return ordersCount;
    }

    public void setOrdersCount(long ordersCount) {
        this.ordersCount = ordersCount;
    }

    public long getDownloadsCount() {
        return downloadsCount;
    }

    public void setDownloadsCount(long downloadsCount) {
        this.downloadsCount = downloadsCount;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public void setDeliveryMethod(String deliveryMethod) {
        this.deliveryMethod = deliveryMethod;
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

    public void applyPendingCheckout(PendingCheckout pending) {
        if (pending == null) {
            return;
        }
        this.deliveryMethod = pending.getDeliveryMethod();
        this.deliveryCharge = pending.getDeliveryCharge() != null ? pending.getDeliveryCharge() : 0.0;
        if (DeliveryMethod.HOME_DELIVERY.name().equalsIgnoreCase(pending.getDeliveryMethod())) {
            this.deliveryAddressId = pending.getDeliveryAddressId();
            this.shippingFullName = pending.getShippingFullName();
            this.shippingMobile = pending.getShippingMobile();
            this.shippingHouseNo = pending.getShippingHouseNo();
            this.shippingBuildingName = pending.getShippingBuildingName();
            this.shippingStreetArea = pending.getShippingStreetArea();
            this.shippingLandmark = pending.getShippingLandmark();
            this.shippingCountry = pending.getShippingCountry();
            this.shippingState = pending.getShippingState();
            this.shippingCity = pending.getShippingCity();
            this.shippingPinCode = pending.getShippingPinCode();
            this.shippingAddressType = pending.getShippingAddressType();
            this.shippingAddressSnapshot = pending.getShippingAddressSnapshot();
        } else if (DeliveryMethod.STORE_PICKUP.name().equalsIgnoreCase(pending.getDeliveryMethod())) {
            this.pickupStoreId = pending.getPickupStoreId();
            this.pickupStoreName = pending.getPickupStoreName();
            this.pickupStoreContact = pending.getPickupStoreContact();
            this.pickupStoreAddress = pending.getPickupStoreAddress();
            this.pickupStoreCity = pending.getPickupStoreCity();
            this.pickupStoreState = pending.getPickupStoreState();
            this.pickupStorePinCode = pending.getPickupStorePinCode();
            this.pickupStoreOpeningHours = pending.getPickupStoreOpeningHours();
            this.pickupInstructions = pending.getPickupInstructions() != null
                    ? pending.getPickupInstructions()
                    : DeliveryConfig.DEFAULT_PICKUP_INSTRUCTIONS;
        }
        if (this.orderStatus == null || this.orderStatus.isBlank() || "CONFIRMED".equalsIgnoreCase(this.orderStatus)) {
            this.orderStatus = "ORDER_PLACED";
        }
    }

    @Transient
    public boolean isHomeDelivery() {
        return DeliveryMethod.HOME_DELIVERY.name().equalsIgnoreCase(deliveryMethod);
    }

    @Transient
    public boolean isStorePickup() {
        return DeliveryMethod.STORE_PICKUP.name().equalsIgnoreCase(deliveryMethod);
    }

    @Transient
    public boolean hasDeliveryDetails() {
        return isHomeDelivery() || isStorePickup();
    }

    @Transient
    public String getDeliveryMethodDisplayName() {
        if (isHomeDelivery()) {
            return "Home Delivery";
        }
        if (isStorePickup()) {
            return "Physical Store Pickup";
        }
        return "Standard / Digital Delivery";
    }

    @Transient
    public String getFormattedStoreLocation() {
        if (!isStorePickup()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (pickupStoreAddress != null && !pickupStoreAddress.isBlank()) {
            sb.append(pickupStoreAddress.trim());
        }
        if (pickupStoreCity != null && !pickupStoreCity.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pickupStoreCity.trim());
        }
        if (pickupStoreState != null && !pickupStoreState.isBlank()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pickupStoreState.trim());
        }
        if (pickupStorePinCode != null && !pickupStorePinCode.isBlank()) {
            sb.append(" - ").append(pickupStorePinCode.trim());
        }
        if (sb.length() > 0) {
            sb.append(", India");
        }
        return sb.toString();
    }

    @Transient
    public String getDisplayOrderStatus() {
        return formatStatusLabel(getOrderStatus());
    }

    public static String formatStatusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Order Placed";
        }
        return switch (status.trim().toUpperCase()) {
            case "ORDER_PLACED" -> "Order Placed";
            case "CONFIRMED" -> "Confirmed";
            case "PROCESSING" -> "Processing";
            case "SHIPPED" -> "Shipped";
            case "OUT_FOR_DELIVERY" -> "Out for Delivery";
            case "DELIVERED" -> "Delivered";
            case "READY_FOR_PICKUP" -> "Ready for Pickup";
            case "COLLECTED" -> "Collected";
            case "COMPLETED" -> "Completed";
            case "CANCELLED" -> "Cancelled";
            case "PENDING" -> "Pending";
            default -> status;
        };
    }

    public static List<String> getAllowedStatusesForMethod(String deliveryMethod) {
        if (DeliveryMethod.HOME_DELIVERY.name().equalsIgnoreCase(deliveryMethod)) {
            return List.of("ORDER_PLACED", "PROCESSING", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED");
        }
        if (DeliveryMethod.STORE_PICKUP.name().equalsIgnoreCase(deliveryMethod)) {
            return List.of("ORDER_PLACED", "PROCESSING", "READY_FOR_PICKUP", "COLLECTED", "CANCELLED");
        }
        return List.of("CONFIRMED", "PROCESSING", "COMPLETED", "CANCELLED");
    }
}
