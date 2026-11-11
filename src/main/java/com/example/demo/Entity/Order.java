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
    private String orderStatus = "CONFIRMED"; // CONFIRMED, PROCESSING, COMPLETED, CANCELLED

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
}
