package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
import com.example.demo.Entity.DeliveryMethod;
import com.example.demo.Entity.PendingCheckout;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeSecretKey;

    @Value("${server.port:8525}")
    private String serverPort;

    private String resolveBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl.trim();
        }
        return "http://localhost:" + serverPort;
    }

    public Session retrieveCheckoutSession(String sessionId) throws StripeException {
        Stripe.apiKey = stripeSecretKey;
        return Session.retrieve(sessionId);
    }

    public String createCheckoutSession(Long bookId, String bookTitle, double price) throws StripeException {
        return createCheckoutSession(bookId, bookTitle, price, 1, null, null, null, null);
    }

    public String createCheckoutSession(Long bookId, String bookTitle, double price, Long userId, String userEmail) throws StripeException {
        return createCheckoutSession(bookId, bookTitle, price, 1, userId, userEmail, null, null);
    }

    public String createCheckoutSession(Long bookId, String bookTitle, double price, Long userId, String userEmail, String baseUrl) throws StripeException {
        return createCheckoutSession(bookId, bookTitle, price, 1, userId, userEmail, baseUrl, null);
    }

    public String createCheckoutSession(
            Long bookId,
            String bookTitle,
            double price,
            int quantity,
            Long userId,
            String userEmail,
            String baseUrl,
            PendingCheckout pendingCheckout) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String resolvedBase = resolveBaseUrl(baseUrl);
        int qty = Math.max(1, quantity);

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(resolvedBase + "/success?session_id={CHECKOUT_SESSION_ID}&bookId=" + bookId)
                .setCancelUrl(resolvedBase + "/checkout/" + bookId + "?paymentCancelled=true")
                .putMetadata("source", "ebookstore")
                .putMetadata("checkoutType", "SINGLE")
                .putMetadata("bookId", String.valueOf(bookId))
                .putMetadata("quantity", String.valueOf(qty))
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity((long) qty)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("INR")
                                                .setUnitAmount(Math.max(100L, Math.round(price * 100.0)))
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(bookTitle)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );

        applyDeliveryMetadataAndLineItem(builder, pendingCheckout);

        if (userId != null) {
            builder.putMetadata("userId", String.valueOf(userId));
        }
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            builder.putMetadata("userEmail", userEmail.trim());
            builder.setCustomerEmail(userEmail.trim());
        }

        Session session = Session.create(builder.build());
        return session.getUrl();
    }

    public String createCheckoutSessionForMultipleBooks(List<Cart> cartItems, double totalAmount) throws StripeException {
        return createCheckoutSessionForMultipleBooks(cartItems, totalAmount, null, null, null, null);
    }

    public String createCheckoutSessionForMultipleBooks(List<Cart> cartItems, double totalAmount, Long userId, String userEmail) throws StripeException {
        return createCheckoutSessionForMultipleBooks(cartItems, totalAmount, userId, userEmail, null, null);
    }

    public String createCheckoutSessionForMultipleBooks(List<Cart> cartItems, double totalAmount, Long userId, String userEmail, String baseUrl) throws StripeException {
        return createCheckoutSessionForMultipleBooks(cartItems, totalAmount, userId, userEmail, baseUrl, null);
    }

    public String createCheckoutSessionForMultipleBooks(
            List<Cart> cartItems,
            double totalAmount,
            Long userId,
            String userEmail,
            String baseUrl,
            PendingCheckout pendingCheckout) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String resolvedBase = resolveBaseUrl(baseUrl);
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        List<String> encodedItems = new ArrayList<>();

        if (cartItems != null) {
            for (Cart cart : cartItems) {
                Book book = cart.getBook();
                if (book != null && !book.isDeleted()) {
                    long qty = Math.max(1L, cart.getQuantity());
                    lineItems.add(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(qty)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("INR")
                                                    .setUnitAmount(Math.max(100L, Math.round(book.getPrice() * 100.0)))
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(book.getTitle())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    );
                    encodedItems.add(book.getId() + ":" + qty);
                }
            }
        }

        if (lineItems.isEmpty()) {
            throw new IllegalArgumentException("No eligible books found in cart for checkout.");
        }

        String cartItemsMeta = encodedItems.stream().collect(Collectors.joining(","));
        if (cartItemsMeta.length() > 490) {
            cartItemsMeta = cartItemsMeta.substring(0, 490);
        }

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(resolvedBase + "/payment-success?session_id={CHECKOUT_SESSION_ID}")
                .setCancelUrl(resolvedBase + "/cart?paymentCancelled=true")
                .addAllLineItem(lineItems)
                .putMetadata("source", "ebookstore")
                .putMetadata("checkoutType", "CART")
                .putMetadata("cartItems", cartItemsMeta);

        applyDeliveryMetadataAndLineItem(builder, pendingCheckout);

        if (userId != null) {
            builder.putMetadata("userId", String.valueOf(userId));
        }
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            builder.putMetadata("userEmail", userEmail.trim());
            builder.setCustomerEmail(userEmail.trim());
        }

        Session session = Session.create(builder.build());
        return session.getUrl();
    }

    private void applyDeliveryMetadataAndLineItem(SessionCreateParams.Builder builder, PendingCheckout pendingCheckout) {
        if (pendingCheckout == null) {
            return;
        }
        if (pendingCheckout.getCheckoutToken() != null && !pendingCheckout.getCheckoutToken().isBlank()) {
            builder.putMetadata("checkoutToken", pendingCheckout.getCheckoutToken().trim());
        }
        if (pendingCheckout.getDeliveryMethod() != null && !pendingCheckout.getDeliveryMethod().isBlank()) {
            builder.putMetadata("deliveryMethod", pendingCheckout.getDeliveryMethod().trim());
        }
        double deliveryCharge = pendingCheckout.getDeliveryCharge() != null ? pendingCheckout.getDeliveryCharge() : 0.0;
        builder.putMetadata("deliveryCharge", String.format("%.2f", deliveryCharge));

        if (deliveryCharge > 0.0) {
            String feeLabel = DeliveryMethod.STORE_PICKUP.name().equalsIgnoreCase(pendingCheckout.getDeliveryMethod())
                    ? "Store Pickup Handling Fee"
                    : "Home Delivery Charge";
            builder.addLineItem(
                    SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(
                                    SessionCreateParams.LineItem.PriceData.builder()
                                            .setCurrency("INR")
                                            .setUnitAmount(Math.max(100L, Math.round(deliveryCharge * 100.0)))
                                            .setProductData(
                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                            .setName(feeLabel)
                                                            .build()
                                            )
                                            .build()
                            )
                            .build()
            );
        }
    }
}
