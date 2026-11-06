
package com.example.demo.service;

import java.time.Year;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.example.demo.Entity.Order;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private final String ADMIN_EMAIL = "playwithhacker@gmail.com";

    /**
     * MODULE 1: Automated Order Receipt Email with Thymeleaf HTML Template & Attached PDF Invoice
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
            helper.setSubject("📖 Order Confirmation & Tax Receipt #" + receiptNum + " - Quickread Bookstore");

            // Build Thymeleaf Context
            Context context = new Context();
            context.setVariable("customerName", order.getUser().getUsername() != null ? order.getUser().getUsername() : "Valued Reader");
            context.setVariable("customerEmail", toEmail);
            context.setVariable("receiptNumber", receiptNum);
            context.setVariable("orderNumber", order.getOrderNumber() != null ? order.getOrderNumber() : ("ORD-" + order.getId()));
            
            String formattedDate = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))
                    : "Recent Purchase";
            context.setVariable("orderDate", formattedDate);
            context.setVariable("paymentStatus", order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID - VERIFIED");
            context.setVariable("stripePaymentReference", order.getStripePaymentIntentId() != null ? order.getStripePaymentIntentId() : order.getStripeSessionId());

            if (order.getBook() != null) {
                context.setVariable("bookTitle", order.getBook().getTitle());
                context.setVariable("bookAuthor", order.getBook().getAuthor());
                context.setVariable("bookCategory", order.getBook().getCategory() != null ? order.getBook().getCategory().getName() : "Curated");
            } else {
                context.setVariable("bookTitle", "Selected Literary Work");
                context.setVariable("bookAuthor", "Curated Collection");
                context.setVariable("bookCategory", "Literature");
            }

            int qty = order.getQuantity() > 0 ? order.getQuantity() : 1;
            double unitPrice = order.getUnitPrice() > 0 ? order.getUnitPrice() : order.getAmount();
            double amount = order.getAmount() > 0 ? order.getAmount() : (unitPrice * qty);

            context.setVariable("quantity", qty);
            context.setVariable("unitPrice", String.format("%.2f", unitPrice));
            context.setVariable("subtotal", String.format("%.2f", amount));
            context.setVariable("amount", String.format("%.2f", amount));
            context.setVariable("libraryUrl", "/order");
            context.setVariable("currentYear", Year.now().getValue());

            // Process Thymeleaf HTML template
            String htmlBody = templateEngine.process("email/order-receipt", context);
            helper.setText(htmlBody, true);

            // Attach PDF invoice if present
            if (pdfInvoice != null && pdfInvoice.length > 0) {
                String filename = "Quickread-Receipt-" + receiptNum + ".pdf";
                helper.addAttachment(filename, new ByteArrayResource(pdfInvoice), "application/pdf");
            }

            mailSender.send(message);
            System.out.println("✅ Automated Order Receipt Email sent to: " + toEmail + " with attached PDF: " + receiptNum);
        } catch (Exception e) {
            System.err.println("Notice: Could not send order receipt email via SMTP (" + e.getMessage() + ")");
            System.out.println("=================================================");
            System.out.println("📧 [QUICKREAD ORDER RECEIPT LOG]");
            System.out.println("Recipient: " + toEmail);
            System.out.println("Receipt #: " + order.getReceiptNumber());
            System.out.println("Amount: ₹" + order.getAmount());
            System.out.println("Book: " + (order.getBook() != null ? order.getBook().getTitle() : "N/A"));
            System.out.println("=================================================");
        }
    }

    /**
     * MODULE 3 & 4: Branded OTP Email Template
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🔑 Quickread Verification Code");

            Context context = new Context();
            context.setVariable("otp", otpCode);
            context.setVariable("email", toEmail);
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/otp-verification", context);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("✅ HTML OTP email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Notice: Could not send OTP email via SMTP (" + e.getMessage() + ")");
            System.out.println("=================================================");
            System.out.println("🔑 [QUICKREAD VERIFICATION OTP]");
            System.out.println("Email: " + toEmail);
            System.out.println("OTP Code: " + otpCode);
            System.out.println("=================================================");
        }
    }

    /**
     * MODULE 4: Branded Password Reset Email Template
     */
    public void sendPasswordResetEmail(String toEmail, String resetCode) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("🔒 Quickread Password Reset Request");

            Context context = new Context();
            context.setVariable("resetCode", resetCode);
            context.setVariable("email", toEmail);
            context.setVariable("currentYear", Year.now().getValue());

            String htmlBody = templateEngine.process("email/password-reset", context);
            helper.setText(htmlBody, true);

            mailSender.send(message);
            System.out.println("✅ HTML Password reset email sent to: " + toEmail);
        } catch (Exception e) {
            System.err.println("Notice: Could not send password reset email via SMTP (" + e.getMessage() + ")");
            System.out.println("=================================================");
            System.out.println("🔒 [QUICKREAD PASSWORD RESET]");
            System.out.println("Email: " + toEmail);
            System.out.println("Reset Code: " + resetCode);
            System.out.println("=================================================");
        }
    }

    /**
     * Backward-compatible simple confirmation
     */
    public void sendOrderConfirmation(String to, String bookTitle, double price) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("📚 Order Confirmation - Quickread Bookstore");

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #2C211B; background: #FAF7F2; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; padding: 25px; background: #FFFFFF; border: 1px solid #E5D9C8; border-radius: 8px; }
                        .header { text-align: center; border-bottom: 2px solid #EFE7DA; padding-bottom: 20px; }
                        .price { color: #8B6B45; font-size: 1.2em; font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h2 style="color: #2C211B; margin: 0;">Quickread Order Confirmation</h2>
                        </div>
                        <div style="margin: 20px 0;">
                            <p>Dear Reader,</p>
                            <p>Thank you for acquiring: <strong>%s</strong></p>
                            <p class="price">Price Paid: ₹%.2f</p>
                            <p>Your digital edition is now accessible in your library.</p>
                        </div>
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
            SimpleMailMessage adminMail = new SimpleMailMessage();
            adminMail.setTo(ADMIN_EMAIL);
            adminMail.setSubject("New Contact Query from " + userName);
            adminMail.setText("User Name: " + userName + "\nEmail: " + userEmail + "\nSubject: " + subject + "\n\nMessage:\n" + message);
            mailSender.send(adminMail);

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
