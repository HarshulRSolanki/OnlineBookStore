package com.example.demo.Controller;

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
     * Backend Stripe Webhook Handler with Idempotency & Multi-Book Support
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
                event = Event.GSON.fromJson(payload, Event.class);
            }
        } catch (Exception e) {
            System.err.println("Webhook signature verification failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Signature verification failed");
        }

        if (event == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid event payload");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            Session session = null;
            if (dataObjectDeserializer.getObject().isPresent()) {
                session = (Session) dataObjectDeserializer.getObject().get();
            }

            if (session != null) {
                if (!OrderReceiptService.isStripePaid(session.getPaymentStatus())) {
                    return ResponseEntity.ok("Session not paid; no receipt issued");
                }

                String sessionId = session.getId();
                String paymentIntentId = session.getPaymentIntent();

                List<Order> existing = orderRepo.findAllByStripeSessionId(sessionId);
                if (!existing.isEmpty()) {
                    return ResponseEntity.ok("Already processed");
                }

                User user = null;
                if (session.getMetadata() != null && session.getMetadata().containsKey("userId")) {
                    try {
                        Long uid = Long.parseLong(session.getMetadata().get("userId"));
                        user = userRepo.findById(uid).orElse(null);
                    } catch (Exception ignored) {}
                }
                if (user == null && session.getCustomerDetails() != null && session.getCustomerDetails().getEmail() != null) {
                    user = userRepo.findByEmailIgnoreCase(session.getCustomerDetails().getEmail());
                }

                if (user == null) {
                    return ResponseEntity.ok("User not resolved");
                }

                String checkoutType = session.getMetadata() != null ? session.getMetadata().get("checkoutType") : null;
                String cartItemsMeta = session.getMetadata() != null ? session.getMetadata().get("cartItems") : null;

                if ("CART".equalsIgnoreCase(checkoutType) || (cartItemsMeta != null && !cartItemsMeta.isBlank())) {
                    orderReceiptService.finalizeMultiBookCheckout(
                            user,
                            null,
                            cartItemsMeta,
                            sessionId,
                            paymentIntentId,
                            session.getPaymentStatus());
                    return ResponseEntity.ok("Processed multi-book cart order");
                } else if (session.getMetadata() != null && session.getMetadata().containsKey("bookId")) {
                    try {
                        Long bid = Long.parseLong(session.getMetadata().get("bookId"));
                        Optional<Book> bookOpt = bookRepo.findById(bid);
                        if (bookOpt.isPresent()) {
                            orderReceiptService.finalizeSingleBookCheckout(
                                    user,
                                    bookOpt.get(),
                                    1,
                                    sessionId,
                                    paymentIntentId,
                                    session.getPaymentStatus());
                            return ResponseEntity.ok("Processed single-book order");
                        }
                    } catch (Exception ignored) {}
                }
            }
        }

        return ResponseEntity.ok("Received");
    }
}
