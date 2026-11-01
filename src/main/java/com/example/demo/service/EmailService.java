package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService 
{
	 @Autowired
	 private JavaMailSender mailSender;
    
		     public void sendOrderConfirmation(String to, String bookTitle, double price) throws MessagingException {
		         MimeMessage message = mailSender.createMimeMessage();
		         MimeMessageHelper helper = new MimeMessageHelper(message, true);
		         
		         helper.setTo(to);
		         helper.setSubject("📚 Order Confirmation - E-Bookstore");

		         String htmlContent = """
		             <!DOCTYPE html>
		             <html>
		             <head>
		                 <meta charset="UTF-8">
		                 <meta name="viewport" content="width=device-width, initial-scale=1.0">
		                 <style>
		                     body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; }
		                     .container { max-width: 600px; margin: 20px auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 8px; }
		                     .header { text-align: center; padding-bottom: 20px; border-bottom: 2px solid #f0f0f0; }
		                     .logo { max-width: 150px; height: auto; }
		                     .content { margin: 25px 0; }
		                     .book-details { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }
		                     .price { color: #2ecc71; font-size: 1.2em; font-weight: bold; }
		                     .footer { text-align: center; padding-top: 20px; color: #666; font-size: 0.9em; }
		                 </style>
		             </head>
		             <body>
		                 <div class="container">
		                     <div class="header">
		                         <img th:src="@{/src/main/resources/static/img/logo.jpg}" class="logo" alt="E-Bookstore Logo">
		                         <h2 style="color: #2c3e50; margin-top: 15px;">Order Confirmation</h2>
		                     </div>
		                     
		                     <div class="content">
		                         <p>Dear Reader,</p>
		                         <p>Thank you for your purchase! Your order has been successfully processed.</p>
		                         
		                         <div class="book-details">
		                             <h3 style="margin-top: 0; color: #34495e;">📖 %s</h3>
		                             <p class="price">Price: ₹%.2f</p>
		                         </div>
		                         
		                         <p>Your eBook is now available in your <a href="[Your Website URL]/library" style="color: #3498db; text-decoration: none;">digital library</a>.</p>
		                         <p>Happy reading! 📚✨</p>
		                     </div>
		                     
		                     <div class="footer">
		                         <p>Best regards,<br>The E-Bookstore Team</p>
		                         <p style="margin-top: 15px;">
		                             <a href="[Your Website URL]" style="color: #3498db; text-decoration: none; margin: 0 10px;">Our Website</a>
		                             <a href="[itechperson985@gmail.com]" style="color: #3498db; text-decoration: none; margin: 0 10px;">Support</a>
		                         </p>
		                         <p style="margin-top: 10px; font-size: 0.8em;">
		                             © %d E-Bookstore. All rights reserved.
		                         </p>
		                     </div>
		                 </div>
		             </body>
		             </html>
		             """.formatted(bookTitle, price, java.time.Year.now().getValue());

		         helper.setText(htmlContent, true);
		         mailSender.send(message);
		     }
		 
		     private final String ADMIN_EMAIL = "playwithhacker@gmail.com";
		     
		     public void sendContactEmail(String userName, String userEmail, String subject, String message) {
		         // Email to Admin
		         SimpleMailMessage adminMail = new SimpleMailMessage();
		         adminMail.setTo(ADMIN_EMAIL);
		         adminMail.setSubject("New Contact Query from " + userName);
		         adminMail.setText("User Name: " + userName + "\nEmail: " + userEmail + "\nSubject: " + subject + "\n\nMessage:\n" + message);
		         mailSender.send(adminMail);

		         // Email to User (Confirmation)
		         SimpleMailMessage userMail = new SimpleMailMessage();
		         userMail.setTo(userEmail);
		         userMail.setSubject("Thank You for Contacting Us!");
		         userMail.setText("Hello " + userName + ",\n\nThank you for reaching out to us. We have received your query:\n\nSubject: " + subject + "\n\nMessage:\n" + message + "\n\nWe will get back to you shortly.\n\nBest regards,\nBook Haven Team");
		         mailSender.send(userMail);
		     }
		     
		     
		     
	    }

