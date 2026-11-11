package com.example.demo.Controller;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import com.example.demo.AuthHelper;
import com.example.demo.Entity.Book;
import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;
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

    @Autowired
    private AuthHelper authHelper;

    private boolean isAuthorizedForOrder(Order order, HttpSession session) {
        if (authHelper.isAdmin(session)) {
            return true;
        }
        User user = authHelper.getLoggedInUser(session);
        return user != null && order.getUser() != null && order.getUser().getUserid().equals(user.getUserid());
    }

    /**
     * View combined order receipt in browser (supports single & multi-book orders)
     */
    @GetMapping("/orders/{id}/receipt")
    public String viewReceipt(@PathVariable Long id, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order receipt not found.");
            return "redirect:/order";
        }

        Order order = orderOpt.get();
        if (!isAuthorizedForOrder(order, session)) {
            return "redirect:/access-denied";
        }

        if (!order.isPaid()) {
            redirectAttributes.addFlashAttribute("error", "Official receipt is only available after verified payment completion.");
            return "redirect:/order";
        }

        List<OrderItem> items = order.getEffectiveItems();
        Map<Long, String> itemImages = new HashMap<>();
        for (OrderItem item : items) {
            Book b = item.getBook();
            if (b != null && b.getCoverImage() != null && b.getCoverImage().length > 0) {
                itemImages.put(b.getId(), "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(b.getCoverImage()));
            }
        }

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("itemImages", itemImages);
        return "User/receipt";
    }

    /**
     * Download print-ready PDF combined tax invoice / receipt
     */
    @GetMapping("/orders/{id}/receipt/download")
    public ResponseEntity<byte[]> downloadPdfReceipt(@PathVariable Long id, HttpSession session) {
        if (!authHelper.isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Order order = orderOpt.get();
        if (!isAuthorizedForOrder(order, session)) {
            return ResponseEntity.status(403).build();
        }

        if (!order.isPaid()) {
            return ResponseEntity.status(400).build();
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
     * Resend receipt email to customer (enforces ownership + paid check)
     */
    @PostMapping("/orders/{id}/receipt/resend")
    public String resendReceiptEmail(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (!authHelper.isAuthenticated(session)) {
            return "redirect:/login";
        }

        Optional<Order> orderOpt = orderRepo.findById(id);
        if (orderOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Order not found.");
            return "redirect:/order";
        }

        Order order = orderOpt.get();
        if (!isAuthorizedForOrder(order, session)) {
            return "redirect:/access-denied";
        }

        if (!order.isPaid()) {
            redirectAttributes.addFlashAttribute("error", "Cannot send receipt for unpaid or failed orders.");
            return "redirect:/order";
        }

        order.setReceiptSent(false);
        orderReceiptService.processAndSendReceipt(order, order.getStripeSessionId(), order.getStripePaymentIntentId());
        redirectAttributes.addFlashAttribute("message",
                "Tax receipt email has been resent to " + (order.getUser() != null ? order.getUser().getEmail() : "your email"));
        return "redirect:/orders/" + id + "/receipt";
    }
}
