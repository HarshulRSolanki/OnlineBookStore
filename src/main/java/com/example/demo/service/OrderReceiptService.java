package com.example.demo.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Order;
import com.example.demo.Repository.OrderRepo;

@Service
public class OrderReceiptService {

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderRepo orderRepo;

    /**
     * Completes order details, generates tax invoice PDF, sends receipt email,
     * and sets idempotent flags to prevent duplicate emails.
     */
    public Order processAndSendReceipt(Order order, String stripeSessionId, String stripePaymentIntentId) {
        if (order == null) {
            return null;
        }

        // 1. Ensure financial and transactional metadata
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }

        if (order.getQuantity() <= 0) {
            order.setQuantity(1);
        }

        if (order.getUnitPrice() <= 0.0 && order.getBook() != null) {
            order.setUnitPrice((double) order.getBook().getPrice());
        }

        if (order.getAmount() <= 0.0) {
            order.setAmount(order.getUnitPrice() * order.getQuantity());
        }

        order.setPaymentStatus("PAID");
        order.setCheckedout(true);

        if (stripeSessionId != null && !stripeSessionId.trim().isEmpty()) {
            order.setStripeSessionId(stripeSessionId.trim());
        }

        if (stripePaymentIntentId != null && !stripePaymentIntentId.trim().isEmpty()) {
            order.setStripePaymentIntentId(stripePaymentIntentId.trim());
        }

        // Save order to ensure ID and pre-persist order/receipt numbers are generated
        order = orderRepo.save(order);

        // Fallback generator if pre-persist did not populate numbers
        if (order.getOrderNumber() == null || order.getOrderNumber().isEmpty()) {
            order.setOrderNumber(String.format("ORD-%s-%04d",
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    order.getId() != null ? order.getId() : (long) (Math.random() * 9000 + 1000)));
        }

        if (order.getReceiptNumber() == null || order.getReceiptNumber().isEmpty()) {
            order.setReceiptNumber(String.format("REC-%s-%04d",
                    LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")),
                    order.getId() != null ? order.getId() : (long) (Math.random() * 9000 + 1000)));
        }

        // 2. Generate PDF and send email only if not yet sent (idempotency protection)
        if (!order.isReceiptSent()) {
            try {
                byte[] pdfBytes = invoiceService.generatePdfInvoice(order);

                // Send branded HTML email with PDF invoice attached
                emailService.sendOrderReceiptEmail(order, pdfBytes);

                order.setReceiptSent(true);
                order = orderRepo.save(order);
                System.out.println("✅ Order receipt email and PDF sent successfully for Order " + order.getOrderNumber());
            } catch (Exception e) {
                System.err.println("⚠️ Warning: Could not send order receipt email (" + e.getMessage() + "). Order saved successfully.");
            }
        } else {
            System.out.println("ℹ️ Receipt already sent for Order " + order.getOrderNumber() + ", skipping duplicate email.");
        }

        return order;
    }
}
