package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Book;
import com.example.demo.Entity.Cart;
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

    public String createCheckoutSession(Long bookId, String bookTitle, double price) throws StripeException {
        return createCheckoutSession(bookId, bookTitle, price, null, null);
    }

    public String createCheckoutSession(Long bookId, String bookTitle, double price, Long userId, String userEmail) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String baseUrl = "http://localhost:" + serverPort;

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/success?session_id={CHECKOUT_SESSION_ID}&bookId=" + bookId)
                .setCancelUrl(baseUrl + "/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("INR")
                                                .setUnitAmount((long) (price * 100))
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(bookTitle)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                );

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
        return createCheckoutSessionForMultipleBooks(cartItems, totalAmount, null, null);
    }

    public String createCheckoutSessionForMultipleBooks(List<Cart> cartItems, double totalAmount, Long userId, String userEmail) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        String baseUrl = "http://localhost:" + serverPort;
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

        for (Cart cart : cartItems) {
            Book book = cart.getBook();
            if (book != null && !book.isDeleted()) {
                lineItems.add(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("INR")
                                .setUnitAmount((long) (book.getPrice() * 100))
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(book.getTitle())
                                        .build()
                                )
                                .build()
                        )
                        .build()
                );
            }
        }

        SessionCreateParams.Builder builder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(baseUrl + "/payment-success?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(baseUrl + "/cart?paymentCancelled=true")
            .addAllLineItem(lineItems)
            .putMetadata("source", "ebookstore");

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
}
