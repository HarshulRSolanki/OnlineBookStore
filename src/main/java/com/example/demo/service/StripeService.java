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

    public String createCheckoutSession(Long bookId, String bookTitle, double price) throws StripeException {
        Stripe.apiKey = stripeSecretKey;

        // Create a Stripe checkout session
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("http://localhost:8525/success?session_id={CHECKOUT_SESSION_ID}&bookId=" + bookId)
                .setCancelUrl("http://localhost:8525/cancel")
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setQuantity(1L)
                                .setPriceData(
                                        SessionCreateParams.LineItem.PriceData.builder()
                                                .setCurrency("INR")
                                                .setUnitAmount((long) (price * 100)) // Stripe works in cents
                                                .setProductData(
                                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                .setName(bookTitle)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .build();

        Session session = Session.create(params);
        return session.getUrl(); // Return Stripe checkout URL
    }
    
    
    public String createCheckoutSessionForMultipleBooks(List<Cart> cartItems, double totalAmount) throws StripeException {
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();

        for (Cart cart : cartItems) {
            Book book = cart.getBook();
            lineItems.add(
                SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency("usd") // or "inr", etc.
                            .setUnitAmount((long) (book.getPrice() * 100)) // Stripe needs amount in cents/paise
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

        SessionCreateParams params = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl("http://localhost:8080/payment-success?session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl("http://localhost:8080/cart?paymentCancelled=true")
            .addAllLineItem(lineItems) 
            .putMetadata("source", "ebookstore")
            .build();

        Session session = Session.create(params);
        return session.getUrl(); // This is the Stripe checkout URL
    }
}
    

