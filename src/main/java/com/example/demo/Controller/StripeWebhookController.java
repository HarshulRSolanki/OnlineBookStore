package com.example.demo.Controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.User;
import com.example.demo.Repository.BookRepo;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.Repository.UserRepo;
import com.example.demo.service.OrderReceiptService;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

@RestController
public class StripeWebhookController {

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BookRepo bookRepo;

    @Autowired
    private OrderReceiptService orderReceiptService;

    /**
     * MODULE 1 & 5: Backend Stripe Webhook Handler with Idempotency Protection
     */
    @PostMapping("/api/webhook/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        Event event;

        try {
            if (webhookSecret != null && !webhookSecret.trim().isEmpty() && sigHeader != null) {
                event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            } else {
                // Parse directly if secret is not configured in local development
                event = Event.GSON.fromJson(payload, Event.class);
            }
        } catch (Exception e) {
            System.err.println("Webhook signature verification failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed");
        }

        if (event == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid event payload");
        }

        // Handle successful checkout session
        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            Session session = null;
            if (dataObjectDeserializer.getObject().isPresent()) {
                session = (Session) dataObjectDeserializer.getObject().get();
            }

            if (session != null) {
                String sessionId = session.getId();
                String paymentIntentId = session.getPaymentIntent();

                // Idempotency check: Have we already processed an order for this session?
                if (orderRepo.findByStripeSessionId(sessionId).isPresent()) {
                    System.out.println("ℹ️ Webhook received for already processed session: " + sessionId + ". Skipping.");
                    return ResponseEntity.ok("Already processed");
                }

                // Retrieve user
                User user = null;
                if (session.getMetadata() != null && session.getMetadata().containsKey("userId")) {
                    try {
                        Long uid = Long.parseLong(session.getMetadata().get("userId"));
                        user = userRepo.findById(uid).orElse(null);
                    } catch (Exception ignored) {}
                }
                if (user == null && session.getCustomerDetails() != null && session.getCustomerDetails().getEmail() != null) {
                    user = userRepo.findByEmail(session.getCustomerDetails().getEmail());
                }

                // Retrieve book if direct purchase
                Book book = null;
                if (session.getMetadata() != null && session.getMetadata().containsKey("bookId")) {
                    try {
                        Long bid = Long.parseLong(session.getMetadata().get("bookId"));
                        book = bookRepo.findById(bid).orElse(null);
                    } catch (Exception ignored) {}
                }

                if (user != null && book != null) {
                    Order order = new Order();
                    order.setUser(user);
                    order.setBook(book);
                    order.setQuantity(1);
                    double price = (double) book.getPrice();
                    order.setUnitPrice(price);
                    order.setAmount(price);
                    order.setPaymentStatus("PAID");
                    order.setCheckedout(true);
                    order.setOrderDate(LocalDateTime.now());
                    order.setStripeSessionId(sessionId);
                    order.setStripePaymentIntentId(paymentIntentId);

                    orderReceiptService.processAndSendReceipt(order, sessionId, paymentIntentId);
                    System.out.println("✅ Webhook created and dispatched order receipt for: " + order.getOrderNumber());
                }
            }
        }

        return ResponseEntity.ok("Received");
    }
}
