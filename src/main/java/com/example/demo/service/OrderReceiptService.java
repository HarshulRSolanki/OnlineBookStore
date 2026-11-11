package com.example.demo.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.Download;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.DownloadRepo;
import com.example.demo.Repository.OrderRepo;

@Service
public class OrderReceiptService {

    public static class CheckoutItemInput {
        private final Book book;
        private final int quantity;

        public CheckoutItemInput(Book book, int quantity) {
            this.book = book;
            this.quantity = quantity > 0 ? quantity : 1;
        }

        public Book getBook() {
            return book;
        }

        public int getQuantity() {
            return quantity;
        }
    }

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private BookRepo bookRepo;

    @Autowired
    private DownloadRepo downloadRepo;

    @Autowired
    private CartService cartService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    public static boolean isStripePaid(String stripePaymentStatus) {
        if (stripePaymentStatus == null) {
            return false;
        }
        String normalized = stripePaymentStatus.trim().toLowerCase();
        return "paid".equals(normalized) || "no_payment_required".equals(normalized) || "success".equals(normalized);
    }

    /**
     * Idempotent single-book order finalization locked outside the DB transaction boundary
     * to prevent duplicate orders/receipts across concurrent callbacks and refreshes.
     */
    public Order finalizeSingleBookCheckout(
            User user,
            Book book,
            int quantity,
            String stripeSessionId,
            String stripePaymentIntentId,
            String stripePaymentStatus) {

        if (!isStripePaid(stripePaymentStatus)) {
            throw new IllegalStateException("Payment has not been completed (status: " + stripePaymentStatus + ").");
        }
        if (user == null || book == null) {
            throw new IllegalArgumentException("User and book are required to finalize order.");
        }

        String lockKey = (stripeSessionId != null && !stripeSessionId.isBlank())
                ? stripeSessionId.trim()
                : ("single-" + user.getUserid() + "-" + book.getId() + "-" + System.nanoTime());

        Object lock = sessionLocks.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            return transactionTemplate.execute(status -> {
                if (stripeSessionId != null && !stripeSessionId.isBlank()) {
                    List<Order> existingOrders = orderRepo.findAllByStripeSessionId(stripeSessionId.trim());
                    if (!existingOrders.isEmpty()) {
                        Order existing = existingOrders.get(0);
                        return processAndSendReceipt(existing, stripeSessionId, stripePaymentIntentId);
                    }
                }

                Book managedBook = bookRepo.findById(book.getId()).orElse(book);
                int qty = Math.max(1, quantity);
                double unitPrice = managedBook.getPrice();
                double subtotal = unitPrice * qty;

                Order order = new Order();
                order.setUser(user);
                order.setBook(managedBook);
                order.setQuantity(qty);
                order.setUnitPrice(unitPrice);
                order.setSubtotal(subtotal);
                order.setTax(0.0);
                order.setDiscount(0.0);
                order.setDeliveryCharge(0.0);
                order.setAmount(subtotal);
                order.setPaymentStatus("PAID");
                order.setOrderStatus("CONFIRMED");
                order.setCheckedout(true);
                order.setOrderDate(LocalDateTime.now());
                order.addItem(new OrderItem(managedBook, qty, unitPrice));

                Order saved = processAndSendReceipt(order, stripeSessionId, stripePaymentIntentId);

                // Update stock and grant digital download entitlement
                int currentStock = managedBook.getStock() != null ? managedBook.getStock() : 100;
                managedBook.setStock(Math.max(0, currentStock - qty));
                bookRepo.save(managedBook);

                if (!downloadRepo.existsByUserAndBook(user, managedBook)) {
                    downloadRepo.save(new Download(user, managedBook));
                }

                return saved;
            });
        }
    }

    /**
     * Idempotent multi-book cart checkout creating ONE Order containing all books in OrderItem rows,
     * ONE combined receipt, ONE PDF invoice, ONE confirmation email, and clearing the user's cart.
     */
    public Order finalizeMultiBookCheckout(
            User user,
            List<Cart> cartItems,
            String metadataCartItems,
            String stripeSessionId,
            String stripePaymentIntentId,
            String stripePaymentStatus) {

        if (!isStripePaid(stripePaymentStatus)) {
            throw new IllegalStateException("Payment has not been completed (status: " + stripePaymentStatus + ").");
        }
        if (user == null) {
            throw new IllegalArgumentException("User is required to finalize multi-book order.");
        }

        String lockKey = (stripeSessionId != null && !stripeSessionId.isBlank())
                ? stripeSessionId.trim()
                : ("cart-" + user.getUserid() + "-" + System.nanoTime());

        Object lock = sessionLocks.computeIfAbsent(lockKey, k -> new Object());
        synchronized (lock) {
            return transactionTemplate.execute(status -> {
                if (stripeSessionId != null && !stripeSessionId.isBlank()) {
                    List<Order> existingOrders = orderRepo.findAllByStripeSessionId(stripeSessionId.trim());
                    if (!existingOrders.isEmpty()) {
                        Order existing = existingOrders.get(0);
                        return processAndSendReceipt(existing, stripeSessionId, stripePaymentIntentId);
                    }
                }

                List<CheckoutItemInput> resolvedItems = resolveCheckoutItems(user, cartItems, metadataCartItems);
                if (resolvedItems.isEmpty()) {
                    throw new IllegalStateException("No purchasable items found for order completion.");
                }

                Order order = new Order();
                order.setUser(user);
                order.setPaymentStatus("PAID");
                order.setOrderStatus("CONFIRMED");
                order.setCheckedout(true);
                order.setOrderDate(LocalDateTime.now());
                order.setTax(0.0);
                order.setDiscount(0.0);
                order.setDeliveryCharge(0.0);

                int totalQty = 0;
                double subtotalSum = 0.0;
                for (CheckoutItemInput entry : resolvedItems) {
                    Book managedBook = bookRepo.findById(entry.getBook().getId()).orElse(entry.getBook());
                    int qty = Math.max(1, entry.getQuantity());
                    double unitPrice = managedBook.getPrice();

                    OrderItem orderItem = new OrderItem(managedBook, qty, unitPrice);
                    order.addItem(orderItem);

                    totalQty += qty;
                    subtotalSum += unitPrice * qty;
                }

                // Populate legacy single-book column ONLY if single title (nullable otherwise)
                if (resolvedItems.size() == 1) {
                    Book single = resolvedItems.get(0).getBook();
                    order.setBook(single);
                    order.setUnitPrice(single.getPrice());
                } else {
                    order.setBook(null);
                    order.setUnitPrice(0.0);
                }

                order.setQuantity(totalQty);
                order.setSubtotal(subtotalSum);
                order.setAmount(Math.max(0.0, subtotalSum + order.getTax() + order.getDeliveryCharge() - order.getDiscount()));

                Order savedOrder = processAndSendReceipt(order, stripeSessionId, stripePaymentIntentId);

                // Decrement stock & grant download access for all ordered books
                for (OrderItem item : savedOrder.getEffectiveItems()) {
                    Book b = item.getBook();
                    if (b != null) {
                        int currentStock = b.getStock() != null ? b.getStock() : 100;
                        b.setStock(Math.max(0, currentStock - item.getQuantity()));
                        bookRepo.save(b);

                        if (!downloadRepo.existsByUserAndBook(user, b)) {
                            downloadRepo.save(new Download(user, b));
                        }
                    }
                }

                // Clear cart once order & entitlements are persisted
                cartService.clearCart(user.getUserid());

                return savedOrder;
            });
        }
    }

    private List<CheckoutItemInput> resolveCheckoutItems(User user, List<Cart> providedCart, String metadataCartItems) {
        List<CheckoutItemInput> result = new ArrayList<>();

        List<Cart> sourceCart = (providedCart != null && !providedCart.isEmpty())
                ? providedCart
                : cartService.getActiveCartItems(user);

        if (sourceCart != null) {
            for (Cart c : sourceCart) {
                if (c.getBook() != null && !c.getBook().isDeleted()) {
                    result.add(new CheckoutItemInput(c.getBook(), Math.max(1, c.getQuantity())));
                }
            }
        }

        // Fallback to Stripe session metadata cartItems ("bookId:qty,...") if cart was already cleared externally
        if (result.isEmpty() && metadataCartItems != null && !metadataCartItems.isBlank()) {
            String[] pairs = metadataCartItems.split(",");
            for (String pair : pairs) {
                String[] parts = pair.trim().split(":");
                if (parts.length >= 1 && !parts[0].isBlank()) {
                    try {
                        Long bookId = Long.parseLong(parts[0].trim());
                        int qty = parts.length > 1 ? Math.max(1, Integer.parseInt(parts[1].trim())) : 1;
                        bookRepo.findById(bookId).ifPresent(book -> {
                            if (!book.isDeleted()) {
                                result.add(new CheckoutItemInput(book, qty));
                            }
                        });
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        return result;
    }

    /**
     * Completes order details, generates ONE combined tax invoice PDF, sends ONE receipt email,
     * and sets idempotent flags to prevent duplicate emails.
     */
    @Transactional
    public Order processAndSendReceipt(Order order, String stripeSessionId, String stripePaymentIntentId) {
        if (order == null) {
            return null;
        }
        if ("FAILED".equalsIgnoreCase(order.getPaymentStatus()) || "CANCELLED".equalsIgnoreCase(order.getPaymentStatus())) {
            throw new IllegalStateException("Cannot generate paid receipt for failed or cancelled payment.");
        }

        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }

        if (order.getItems() != null && !order.getItems().isEmpty()) {
            int totalQty = 0;
            double subtotalSum = 0.0;
            for (OrderItem item : order.getItems()) {
                totalQty += item.getQuantity();
                subtotalSum += item.getSubtotal();
            }
            if (totalQty > 0) {
                order.setQuantity(totalQty);
            }
            if (order.getSubtotal() <= 0.0) {
                order.setSubtotal(subtotalSum);
            }
        } else {
            if (order.getQuantity() <= 0) {
                order.setQuantity(1);
            }
            if (order.getUnitPrice() <= 0.0 && order.getBook() != null) {
                order.setUnitPrice(order.getBook().getPrice());
            }
            if (order.getSubtotal() <= 0.0) {
                order.setSubtotal(order.getUnitPrice() * order.getQuantity());
            }
        }

        if (order.getAmount() <= 0.0) {
            order.setAmount(Math.max(0.0,
                    order.getSubtotal() + order.getTax() + order.getDeliveryCharge() - order.getDiscount()));
        }

        order.setPaymentStatus("PAID");
        if (order.getOrderStatus() == null || order.getOrderStatus().isBlank() || "PENDING".equalsIgnoreCase(order.getOrderStatus())) {
            order.setOrderStatus("CONFIRMED");
        }
        order.setCheckedout(true);

        if (stripeSessionId != null && !stripeSessionId.trim().isEmpty()) {
            order.setStripeSessionId(stripeSessionId.trim());
        }

        if (stripePaymentIntentId != null && !stripePaymentIntentId.trim().isEmpty()) {
            order.setStripePaymentIntentId(stripePaymentIntentId.trim());
        }

        order = orderRepo.save(order);

        if (order.getOrderNumber() == null || order.getOrderNumber().isBlank()) {
            order.setOrderNumber(String.format("ORD-%s-%04d",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    order.getId() != null ? order.getId() : (long) (Math.random() * 9000 + 1000)));
        }

        if (order.getReceiptNumber() == null || order.getReceiptNumber().isBlank()) {
            order.setReceiptNumber(String.format("REC-%s-%04d",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                    order.getId() != null ? order.getId() : (long) (Math.random() * 9000 + 1000)));
        }

        if (!order.isReceiptSent()) {
            try {
                byte[] pdfBytes = invoiceService.generatePdfInvoice(order);
                emailService.sendOrderReceiptEmail(order, pdfBytes);
                order.setReceiptSent(true);
                order = orderRepo.save(order);
            } catch (Exception e) {
                System.err.println("Warning: Could not send order receipt email (" + e.getMessage() + "). Order saved.");
            }
        }

        return order;
    }

    /**
     * Parses Stripe cartItems metadata string ("1:2,5:1") cleanly if needed externally.
     */
    public Map<Long, Integer> parseMetadataCartItems(String metadataCartItems) {
        Map<Long, Integer> map = new java.util.LinkedHashMap<>();
        if (metadataCartItems == null || metadataCartItems.isBlank()) {
            return map;
        }
        for (String pair : metadataCartItems.split(",")) {
            String[] parts = pair.trim().split(":");
            if (parts.length >= 1 && !parts[0].isBlank()) {
                try {
                    long id = Long.parseLong(parts[0].trim());
                    int qty = parts.length > 1 ? Math.max(1, Integer.parseInt(parts[1].trim())) : 1;
                    map.put(id, qty);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return map;
    }
}
