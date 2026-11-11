package com.example.demo.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.AuthHelper;
import com.example.demo.Entity.Order;
import com.example.demo.Repository.CategoryRepo;
import com.example.demo.service.SalesReportService;

import jakarta.servlet.http.HttpSession;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminReportController {

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private CategoryRepo categoryRepo;

    @Autowired
    private AuthHelper authHelper;

    private boolean isAdmin(HttpSession session) {
        return authHelper.isAdmin(session);
    }

    @GetMapping("/admin/reports")
    public String showSalesReport(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) String paymentStatus,
            HttpSession session,
            Model model) {

        if (!isAdmin(session)) {
            return "redirect:/access-denied";
        }

        Map<String, Object> stats = salesReportService.getDashboardStats();
        model.addAttribute("stats", stats);

        List<Order> filteredOrders = salesReportService.getFilteredOrders(startDate, endDate, categoryId, bookName, paymentStatus);
        model.addAttribute("orders", filteredOrders);

        model.addAttribute("categories", categoryRepo.findAll());

        model.addAttribute("filterStartDate", startDate != null ? startDate : "");
        model.addAttribute("filterEndDate", endDate != null ? endDate : "");
        model.addAttribute("filterCategoryId", categoryId != null ? categoryId : 0L);
        model.addAttribute("filterBookName", bookName != null ? bookName : "");
        model.addAttribute("filterPaymentStatus", paymentStatus != null ? paymentStatus : "ALL");

        return "Admin/reports";
    }

    @GetMapping("/admin/reports/analytics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getChartAnalytics(HttpSession session) {
        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(salesReportService.getChartAnalytics());
    }

    @GetMapping("/admin/reports/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) String paymentStatus,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        List<Order> orders = salesReportService.getFilteredOrders(startDate, endDate, categoryId, bookName, paymentStatus);
        String filterDesc = buildFilterDescription(startDate, endDate, categoryId, bookName, paymentStatus);
        byte[] pdfBytes = salesReportService.exportSalesPdf(orders, filterDesc);

        String filename = "Quickread-Sales-Report-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/admin/reports/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) String paymentStatus,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        List<Order> orders = salesReportService.getFilteredOrders(startDate, endDate, categoryId, bookName, paymentStatus);
        String filterDesc = buildFilterDescription(startDate, endDate, categoryId, bookName, paymentStatus);
        byte[] xlsxBytes = salesReportService.exportSalesExcel(orders, filterDesc);

        String filename = "Quickread-Sales-Report-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsxBytes);
    }

    @GetMapping("/admin/reports/export/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String bookName,
            @RequestParam(required = false) String paymentStatus,
            HttpSession session) {

        if (!isAdmin(session)) {
            return ResponseEntity.status(403).build();
        }

        List<Order> orders = salesReportService.getFilteredOrders(startDate, endDate, categoryId, bookName, paymentStatus);
        byte[] csvBytes = salesReportService.exportSalesCsv(orders);

        String filename = "Quickread-Sales-Report-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }

    private String buildFilterDescription(String startDate, String endDate, Long categoryId, String bookName, String paymentStatus) {
        StringBuilder sb = new StringBuilder("Records Filter: ");
        if (startDate != null && !startDate.isEmpty()) sb.append("From: ").append(startDate).append(" ");
        if (endDate != null && !endDate.isEmpty()) sb.append("To: ").append(endDate).append(" ");
        if (paymentStatus != null && !paymentStatus.equalsIgnoreCase("ALL")) sb.append("Status: ").append(paymentStatus).append(" ");
        if (bookName != null && !bookName.isEmpty()) sb.append("Title: '").append(bookName).append("' ");
        return sb.toString().trim();
    }
}
