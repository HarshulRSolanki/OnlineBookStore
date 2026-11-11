package com.example.demo.service;

import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    @Value("${app.contact.admin-email:${spring.mail.username:support@quickread.local}}")
    private String contactAdminEmail;

    /**
     * Automated Combined Order Receipt Email with Thymeleaf HTML Template & Attached PDF Invoice
     */
    public void sendOrderReceiptEmail(Order order, byte[] pdfInvoice) {
        if (order == null || order.getUser() == null || order.getUser().getEmail() == null) {
            System.err.println("Cannot send order receipt email: Order or customer email is null.");
            return;
        }

        String toEmail = order.getUser().getEmail();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            String receiptNum = order.getReceiptNumber() != null ? order.getReceiptNumber() : ("REC-" + order.getId());
            helper.setSubject("Order Confirmation & Receipt #" + receiptNum + " - Quickread Bookstore");

            Context context = new Context();
            context.setVariable("customerName", order.getUser().getUsername() != null ? order.getUser().getUsername() : "Valued Reader");
            context.setVariable("customerEmail", toEmail);
            context.setVariable("receiptNumber", receiptNum);
            context.setVariable("orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : ("ORD-" + order.getId()));

            String formattedDate = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))
                    : "Recent Purchase";
            context.setVariable("orderDate", formattedDate);
            context.setVariable("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID");
            context.setVariable("stripePaymentReference",
                    order.getStripePaymentIntentId() != null && !order.getStripePaymentIntentId().isBlank()
                            ? order.getStripePaymentIntentId()
                            : order.getStripeSessionId());

            List<OrderItem> items = order.getEffectiveItems();
            context.setVariable("items", items);
            context.setVariable("distinctBookCount", items.size());
            context.setVariable("totalItemCount", order.getTotalItemQuantity());

            if (items.size() == 1) {
                OrderItem single = items.get(0);
                context.setVariable("bookTitle", single.getEffectiveTitle());
                context.setVariable("bookAuthor", single.getEffectiveAuthor());
                context.setVariable("bookCategory", single.getEffectiveCategory());
                context.setVariable("unitPrice", String.format("%.2f", single.getUnitPrice()));
            } else {
                context.setVariable("bookTitle", order.getSummaryTitle());
                context.setVariable("bookAuthor", "Curated Collection (" + items.size() + " books)");
                context.setVariable("bookCategory", "Multi-Book Order");
                context.setVariable("unitPrice", String.format("%.2f", order.getSubtotal()));
            }

            context.setVariable("quantity", order.getTotalItemQuantity());
            context.setVariable("subtotal", String.format("%.2f", order.getSubtotal()));
            context.setVariable("tax", String.format("%.2f", order.getTax()));
            context.setVariable("discount", String.format("%.2f", order.getDiscount()));
            context.setVariable("deliveryCharge", String.format("%.2f", order.getDeliveryCharge()));
            context.setVariable("amount", String.format("%.2f", order.getAmount()));
            context.setVariable("libraryUrl", "/order");
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/order-receipt", context);
            helper.setText(htmlBody, true);

            if (pdfInvoice != null && pdfInvoice.length > 0) {
                String filename = "Quickread-Receipt-" + receiptNum + ".pdf";
                helper.addAttachment(filename, new ByteArrayResource(pdfInvoice), "application/pdf");
            }

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Notice: Could not send order receipt email via SMTP (" + e.getMessage() + ")");
        }
    }

    /**
     * Branded OTP Email Template
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        sendOtpEmail(
                toEmail,
                otpCode,
                "Quickread Verification Code",
                "Verification Code",
                "Use the 6-digit security code below to verify your email address.");
    }

    public void sendOtpEmail(String toEmail, String otpCode, String subject, String heading, String instructions) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject(subject != null ? subject : "Quickread Verification Code");

            Context context = new Context();
            context.setVariable("otp", otpCode);
            context.setVariable("email", toEmail);
            context.setVariable("heading", heading != null ? heading : "Verification Code");
            context.setVariable("instructions", instructions != null ? instructions : "Use the 6-digit security code below to complete your verification.");
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/otp-verification", context);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Notice: Could not send OTP email via SMTP (" + e.getMessage() + ")");
        }
    }

    /**
     * Branded Password Reset Email Template
     */
    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Quickread Password Reset Request");

            Context context = new Context();
            context.setVariable("resetCode", resetCode);
            context.setVariable("email", toEmail);
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/password-reset", context);
            helper.setText(htmlBody, true);

            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Notice: Could not send password reset email via SMTP (" + e.getMessage() + ")");
        }
    }

    public void sendOrderConfirmation(String to, String bookTitle, double price) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("Order Confirmation - Quickread Bookstore");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Segoe UI, sans-serif; color: #2C211B; background: #FAF7F2; padding: 20px;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 25px; background: #FFFFFF; border: 1px solid #E5D9C8; border-radius: 8px;">
                        <h2 style="color: #2C211B; margin-top: 0;">Quickread Order Confirmation</h2>
                        <p>Dear Reader,</p>
                        <p>Thank you for acquiring: <strong>%s</strong></p>
                        <p style="color: #8B6B45; font-size: 1.1em; font-weight: bold;">Amount Paid: INR %.2f</p>
                        <p>Your digital edition is now accessible in your library.</p>
                    </div>
                </body>
                </html>
                """.formatted(bookTitle, price);

            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Notice: Simple confirmation fallback: " + e.getMessage());
        }
    }

    public void sendContactEmail(String userName, String userEmail, String subject, String message) {
        try {
            if (contactAdminEmail != null && !contactAdminEmail.isBlank() && !contactAdminEmail.endsWith(".local")) {
                SimpleMailMessage adminMail = new SimpleMailMessage();
                adminMail.setTo(contactAdminEmail);
                adminMail.setSubject("New Contact Query from " + userName);
                adminMail.setText("User Name: " + userName + "\nEmail: " + userEmail + "\nSubject: " + subject + "\n\nMessage:\n" + message);
                mailSender.send(adminMail);
            }

            SimpleMailMessage userMail = new SimpleMailMessage();
            userMail.setTo(userEmail);
            userMail.setSubject("Thank You for Contacting Quickread!");
            userMail.setText("Hello " + userName + ",\n\nThank you for reaching out to us. We have received your query:\n\nSubject: " + subject + "\n\nMessage:\n" + message + "\n\nWe will get back to you shortly.\n\nBest regards,\nQuickread Curatorial Team");
            mailSender.send(userMail);
        } catch (Exception e) {
            System.err.println("Notice: Could not send contact inquiry email via SMTP: " + e.getMessage());
        }
    }
}
