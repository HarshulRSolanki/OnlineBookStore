package com.example.demo.Controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Entity.Order;
import com.example.demo.Entity.User;
import com.example.demo.Repository.OrderRepo;
import com.example.demo.service.InvoiceService;
import com.example.demo.service.OrderReceiptService;

import jakarta.servlet.http.HttpSession;

@Controller
public class OrderReceiptController {

    @Autowired
    private OrderRepo orderRepo;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private OrderReceiptService orderReceiptService;

    /**
     * View customer receipt in web browser
     */
    @GetMapping("/orders/{id}/receipt")
    public String viewReceipt(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Object sessionUser = session.getAttribute("loggedInUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order receipt not found.");
            return "redirect:/order";
        }

        Order order = orderOpt.get();

        // Authorization check: User must own the order or be Admin
        boolean isAdmin = "Admin".equalsIgnoreCase(sessionUser.toString());
        if (!isAdmin && sessionUser instanceof User) {
            User user = (User) sessionUser;
            if (order.getUser() == null || !order.getUser().getUserid().equals(user.getUserid())) {
                redirectAttributes.addFlashAttribute("error", "Unauthorized access to order receipt.");
                return "redirect:/order";
            }
        }

        model.addAttribute("order", order);
        return "User/receipt";
    }

    /**
     * Download print-ready PDF tax invoice / receipt
     */
    @GetMapping("/orders/{id}/receipt/download")
    public ResponseEntity<byte[]> downloadPdfReceipt(@PathVariable Long id, HttpSession session) {
        Object sessionUser = session.getAttribute("loggedInUser");
        if (sessionUser == null) {
            return ResponseEntity.status(401).build();
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();

        // Authorization check
        boolean isAdmin = "Admin".equalsIgnoreCase(sessionUser.toString());
        if (!isAdmin && sessionUser instanceof User) {
            User user = (User) sessionUser;
            if (order.getUser() == null || !order.getUser().getUserid().equals(user.getUserid())) {
                return ResponseEntity.status(403).build();
            }
        }

        byte[] pdfBytes = invoiceService.generatePdfInvoice(order);
        String receiptNum = order.getReceiptNumber() != null ? order.getReceiptNumber() : ("REC-" + order.getId());
        String filename = "Quickread-Receipt-" + receiptNum + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    /**
     * Resend receipt email to customer
     */
    @PostMapping("/orders/{id}/receipt/resend")
    public String resendReceiptEmail(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        Object sessionUser = session.getAttribute("loggedInUser");
        if (sessionUser == null) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            order.setReceiptSent(false); // reset flag to trigger resend
            orderReceiptService.processAndSendReceipt(order, order.getStripeSessionId(), order.getStripePaymentIntentId());
            redirectAttributes.addFlashAttribute("message", "Tax receipt email has been resent to " + order.getUser().getEmail());
        } else {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
        }

        return "redirect:/order";
    }
}

