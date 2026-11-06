package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.Entity.Order;
import com.example.demo.Repository.OrderRepo;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class SalesReportService {

    @Autowired
    private OrderRepo orderRepo;

    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(44, 33, 27);      // #2C211B Espresso
    private static final DeviceRgb COLOR_ACCENT = new DeviceRgb(139, 107, 69);     // #8B6B45 Gold/Tan
    private static final DeviceRgb COLOR_BORDER = new DeviceRgb(229, 217, 200);    // #E5D9C8
    private static final DeviceRgb COLOR_BG_LIGHT = new DeviceRgb(250, 247, 242);  // Cream

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Order> allOrders = orderRepo.findAllByOrderByOrderDateDesc();
        long totalOrders = allOrders.size();

        double totalRevenue = 0.0;
        long totalBooksSold = 0;
        long successfulPayments = 0;
        long pendingOrders = 0;
        long cancelledOrders = 0;

        Map<String, Long> bookSalesCount = new HashMap<>();

        for (Order o : allOrders) {
            String status = o.getPaymentStatus() != null ? o.getPaymentStatus().toUpperCase() : "PENDING";
            if ("SUCCESS".equals(status) || "PAID".equals(status)) {
                successfulPayments++;
                totalRevenue += o.getAmount();
                totalBooksSold += o.getQuantity();

                if (o.getBook() != null) {
                    String title = o.getBook().getTitle();
                    bookSalesCount.put(title, bookSalesCount.getOrDefault(title, 0L) + o.getQuantity());
                }
            } else if ("PENDING".equals(status)) {
                pendingOrders++;
            } else if ("FAILED".equals(status) || "CANCELLED".equals(status)) {
                cancelledOrders++;
            }
        }

        double aov = successfulPayments > 0 ? (totalRevenue / successfulPayments) : 0.0;

        String bestSeller = "N/A";
        long bestSellerCount = 0;
        for (Map.Entry<String, Long> entry : bookSalesCount.entrySet()) {
            if (entry.getValue() > bestSellerCount) {
                bestSellerCount = entry.getValue();
                bestSeller = entry.getKey() + " (" + bestSellerCount + " copies)";
            }
        }

        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", totalOrders);
        stats.put("totalBooksSold", totalBooksSold);
        stats.put("averageOrderValue", aov);
        stats.put("successfulPayments", successfulPayments);
        stats.put("pendingOrders", pendingOrders);
        stats.put("cancelledOrders", cancelledOrders);
        stats.put("bestSellingBook", bestSeller);

        return stats;
    }

    public Map<String, Object> getChartAnalytics() {
        Map<String, Object> analytics = new HashMap<>();

        List<Order> paidOrders = orderRepo.findAllByOrderByOrderDateDesc().stream()
                .filter(o -> {
                    String s = o.getPaymentStatus() != null ? o.getPaymentStatus().toUpperCase() : "";
                    return "SUCCESS".equals(s) || "PAID".equals(s);
                })
                .collect(Collectors.toList());

        // 1. Daily Sales (Last 7 Days)
        Map<String, Double> dailySales = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate d = today.minusDays(i);
            dailySales.put(d.format(DateTimeFormatter.ofPattern("dd MMM")), 0.0);
        }
        for (Order o : paidOrders) {
            if (o.getOrderDate() != null) {
                LocalDate d = o.getOrderDate().toLocalDate();
                String key = d.format(DateTimeFormatter.ofPattern("dd MMM"));
                if (dailySales.containsKey(key)) {
                    dailySales.put(key, dailySales.get(key) + o.getAmount());
                }
            }
        }
        analytics.put("dailyLabels", new ArrayList<>(dailySales.keySet()));
        analytics.put("dailyValues", new ArrayList<>(dailySales.values()));

        // 2. Monthly Sales (Last 6 Months)
        Map<String, Double> monthlySales = new LinkedHashMap<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate m = today.minusMonths(i);
            monthlySales.put(m.format(DateTimeFormatter.ofPattern("MMM yyyy")), 0.0);
        }
        for (Order o : paidOrders) {
            if (o.getOrderDate() != null) {
                String key = o.getOrderDate().format(DateTimeFormatter.ofPattern("MMM yyyy"));
                if (monthlySales.containsKey(key)) {
                    monthlySales.put(key, monthlySales.get(key) + o.getAmount());
                }
            }
        }
        analytics.put("monthlyLabels", new ArrayList<>(monthlySales.keySet()));
        analytics.put("monthlyValues", new ArrayList<>(monthlySales.values()));

        // 3. Revenue by Book Category
        Map<String, Double> categoryRevenue = new HashMap<>();
        for (Order o : paidOrders) {
            if (o.getBook() != null && o.getBook().getCategory() != null) {
                String cat = o.getBook().getCategory().getName();
                categoryRevenue.put(cat, categoryRevenue.getOrDefault(cat, 0.0) + o.getAmount());
            } else {
                categoryRevenue.put("Uncategorized", categoryRevenue.getOrDefault("Uncategorized", 0.0) + o.getAmount());
            }
        }
        analytics.put("categoryLabels", new ArrayList<>(categoryRevenue.keySet()));
        analytics.put("categoryValues", new ArrayList<>(categoryRevenue.values()));

        // 4. Order Status Breakdown
        long success = orderRepo.findAll().stream().filter(o -> {
            String s = o.getPaymentStatus() != null ? o.getPaymentStatus().toUpperCase() : "";
            return "SUCCESS".equals(s) || "PAID".equals(s);
        }).count();
        long pending = orderRepo.findAll().stream().filter(o -> "PENDING".equalsIgnoreCase(o.getPaymentStatus())).count();
        long cancelled = orderRepo.findAll().stream().filter(o -> {
            String s = o.getPaymentStatus() != null ? o.getPaymentStatus().toUpperCase() : "";
            return "CANCELLED".equals(s) || "FAILED".equals(s);
        }).count();

        analytics.put("statusLabels", List.of("Paid / Successful", "Pending", "Cancelled / Failed"));
        analytics.put("statusValues", List.of(success, pending, cancelled));

        return analytics;
    }

    public List<Order> getFilteredOrders(String startDate, String endDate, Long categoryId, String bookName, String paymentStatus) {
        List<Order> orders = orderRepo.findAllByOrderByOrderDateDesc();

        return orders.stream().filter(o -> {
            if (startDate != null && !startDate.trim().isEmpty()) {
                try {
                    LocalDate start = LocalDate.parse(startDate.trim());
                    if (o.getOrderDate() != null && o.getOrderDate().toLocalDate().isBefore(start)) {
                        return false;
                    }
                } catch (Exception ignored) {}
            }
            if (endDate != null && !endDate.trim().isEmpty()) {
                try {
                    LocalDate end = LocalDate.parse(endDate.trim());
                    if (o.getOrderDate() != null && o.getOrderDate().toLocalDate().isAfter(end)) {
                        return false;
                    }
                } catch (Exception ignored) {}
            }
            if (categoryId != null && categoryId > 0) {
                if (o.getBook() == null || o.getBook().getCategory() == null || !o.getBook().getCategory().getId().equals(categoryId)) {
                    return false;
                }
            }
            if (bookName != null && !bookName.trim().isEmpty()) {
                if (o.getBook() == null || !o.getBook().getTitle().toLowerCase().contains(bookName.trim().toLowerCase())) {
                    return false;
                }
            }
            if (paymentStatus != null && !paymentStatus.trim().isEmpty() && !"ALL".equalsIgnoreCase(paymentStatus.trim())) {
                if (o.getPaymentStatus() == null || !o.getPaymentStatus().equalsIgnoreCase(paymentStatus.trim())) {
                    return false;
                }
            }
            return true;
        }).collect(Collectors.toList());
    }

    public byte[] exportSalesPdf(List<Order> orders, String filterDescription) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4.rotate()); // Landscape for tabular analytics

            Document document = new Document(pdfDoc);
            document.setMargins(28, 28, 28, 28);

            // 1. Header
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{65, 35}));
            headerTable.setWidth(UnitValue.createPercentValue(100));

            Cell brand = new Cell().setBorder(Border.NO_BORDER);
            brand.add(new Paragraph("QUICKREAD BOOKSTORE").setFontSize(18).setBold().setFontColor(COLOR_PRIMARY));
            brand.add(new Paragraph("Official Curatorial Sales Analytics & Financial Ledger").setFontSize(9).setFontColor(COLOR_ACCENT));
            brand.add(new Paragraph("Scope: " + (filterDescription != null ? filterDescription : "Comprehensive All-Time Records")).setFontSize(8).setFontColor(COLOR_PRIMARY));
            headerTable.addCell(brand);

            Cell meta = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            meta.add(new Paragraph("AUDIT REPORT").setFontSize(14).setBold().setFontColor(COLOR_PRIMARY));
            meta.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))).setFontSize(8));
            meta.add(new Paragraph("Total Records: " + orders.size()).setFontSize(8).setBold());
            headerTable.addCell(meta);

            document.add(headerTable);
            document.add(new Paragraph("\n").setFontSize(6));

            // Summary Metrics Row
            double totalRev = orders.stream().filter(o -> "SUCCESS".equalsIgnoreCase(o.getPaymentStatus()) || "PAID".equalsIgnoreCase(o.getPaymentStatus())).mapToDouble(Order::getAmount).sum();
            long totalSold = orders.stream().filter(o -> "SUCCESS".equalsIgnoreCase(o.getPaymentStatus()) || "PAID".equalsIgnoreCase(o.getPaymentStatus())).mapToLong(Order::getQuantity).sum();

            Table summaryMetrics = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}));
            summaryMetrics.setWidth(UnitValue.createPercentValue(100));
            summaryMetrics.setBackgroundColor(COLOR_BG_LIGHT);

            addStatBox(summaryMetrics, "TOTAL REVENUE", String.format("₹%.2f", totalRev));
            addStatBox(summaryMetrics, "TOTAL VOLUMES SOLD", String.valueOf(totalSold));
            addStatBox(summaryMetrics, "RECORDED ORDERS", String.valueOf(orders.size()));
            addStatBox(summaryMetrics, "AVERAGE BASKET", String.format("₹%.2f", orders.size() > 0 ? (totalRev / orders.size()) : 0.0));

            document.add(summaryMetrics);
            document.add(new Paragraph("\n").setFontSize(8));

            // Table of Orders
            Table dataTable = new Table(UnitValue.createPercentArray(new float[]{12, 16, 22, 12, 6, 10, 10, 12}));
            dataTable.setWidth(UnitValue.createPercentValue(100));

            String[] tableHeaders = {"Order #", "Customer", "Book Title", "Category", "Qty", "Unit Price", "Total (₹)", "Date"};
            for (String th : tableHeaders) {
                Cell thCell = new Cell().setBackgroundColor(COLOR_PRIMARY).setPadding(6);
                thCell.add(new Paragraph(th).setFontSize(8).setBold().setFontColor(ColorConstants.WHITE));
                if (th.equals("Qty") || th.contains("Price") || th.contains("Total")) {
                    thCell.setTextAlignment(TextAlignment.RIGHT);
                }
                dataTable.addHeaderCell(thCell);
            }

            for (Order o : orders) {
                dataTable.addCell(createCell(o.getOrderNumber() != null ? o.getOrderNumber() : "ORD-" + o.getId()));
                dataTable.addCell(createCell(o.getUser() != null ? o.getUser().getUsername() : "Reader"));
                dataTable.addCell(createCell(o.getBook() != null ? o.getBook().getTitle() : "Edition"));
                dataTable.addCell(createCell(o.getBook() != null && o.getBook().getCategory() != null ? o.getBook().getCategory().getName() : "-"));
                dataTable.addCell(createCell(String.valueOf(o.getQuantity()), TextAlignment.RIGHT));
                dataTable.addCell(createCell(String.format("₹%.2f", o.getUnitPrice()), TextAlignment.RIGHT));
                dataTable.addCell(createCell(String.format("₹%.2f", o.getAmount()), TextAlignment.RIGHT));
                dataTable.addCell(createCell(o.getOrderDate() != null ? o.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yy")) : "-"));
            }

            document.add(dataTable);

            // Footer
            document.add(new Paragraph("\n").setFontSize(6));
            Paragraph footer = new Paragraph("Quickread Curatorial Intelligence System • Confidential Administrative Document • Page 1")
                    .setFontSize(7).setFontColor(COLOR_ACCENT).setTextAlignment(TextAlignment.CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Sales PDF: " + e.getMessage(), e);
        }
    }

    private void addStatBox(Table table, String label, String value) {
        Cell cell = new Cell().setPadding(8).setBorder(new SolidBorder(COLOR_BORDER, 1));
        cell.add(new Paragraph(label).setFontSize(7).setFontColor(COLOR_ACCENT).setBold());
        cell.add(new Paragraph(value).setFontSize(11).setFontColor(COLOR_PRIMARY).setBold());
        table.addCell(cell);
    }

    private Cell createCell(String text) {
        return createCell(text, TextAlignment.LEFT);
    }

    private Cell createCell(String text, TextAlignment align) {
        Cell c = new Cell().setPadding(5).setTextAlignment(align).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
        c.add(new Paragraph(text != null ? text : "-").setFontSize(8).setFontColor(COLOR_PRIMARY));
        return c;
    }

    public byte[] exportSalesExcel(List<Order> orders, String filterDescription) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Sales Report");

            // Header Style
            XSSFCellStyle headerStyle = (XSSFCellStyle) workbook.createCellStyle();
            XSSFFont headerFont = (XSSFFont) workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.BROWN.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // Title Row
            Row titleRow = sheet.createRow(0);
            titleRow.createCell(0).setCellValue("QUICKREAD BOOKSTORE - SALES & FINANCIAL REPORT");

            Row filterRow = sheet.createRow(1);
            filterRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) + " | Scope: " + (filterDescription != null ? filterDescription : "All Records"));

            sheet.createRow(2); // Empty spacer

            // Table Column Headers
            String[] cols = {"Order ID", "Receipt #", "Customer Name", "Customer Email", "Book Title", "Category", "Quantity", "Unit Price (INR)", "Total Amount (INR)", "Payment Status", "Order Date"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < cols.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 4;
            double totalRevenue = 0.0;
            int totalBooks = 0;

            for (Order o : orders) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(o.getOrderNumber() != null ? o.getOrderNumber() : ("ORD-" + o.getId()));
                row.createCell(1).setCellValue(o.getReceiptNumber() != null ? o.getReceiptNumber() : ("REC-" + o.getId()));
                row.createCell(2).setCellValue(o.getUser() != null ? o.getUser().getUsername() : "Reader");
                row.createCell(3).setCellValue(o.getUser() != null ? o.getUser().getEmail() : "");
                row.createCell(4).setCellValue(o.getBook() != null ? o.getBook().getTitle() : "");
                row.createCell(5).setCellValue(o.getBook() != null && o.getBook().getCategory() != null ? o.getBook().getCategory().getName() : "");
                row.createCell(6).setCellValue(o.getQuantity());
                row.createCell(7).setCellValue(o.getUnitPrice());
                row.createCell(8).setCellValue(o.getAmount());
                row.createCell(9).setCellValue(o.getPaymentStatus() != null ? o.getPaymentStatus() : "PAID");
                row.createCell(10).setCellValue(o.getOrderDate() != null ? o.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");

                totalRevenue += o.getAmount();
                totalBooks += o.getQuantity();
            }

            // Total Summary Row
            Row summaryRow = sheet.createRow(rowIdx + 1);
            summaryRow.createCell(0).setCellValue("TOTAL SUMMARY");
            summaryRow.createCell(6).setCellValue(totalBooks);
            summaryRow.createCell(8).setCellValue(totalRevenue);

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report: " + e.getMessage(), e);
        }
    }

    public byte[] exportSalesCsv(List<Order> orders) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); PrintWriter pw = new PrintWriter(baos)) {
            pw.println("Order Number,Receipt Number,Customer Name,Customer Email,Book Title,Category,Quantity,Unit Price,Total Amount,Payment Status,Order Date");

            for (Order o : orders) {
                String ordNo = escapeCsv(o.getOrderNumber() != null ? o.getOrderNumber() : ("ORD-" + o.getId()));
                String recNo = escapeCsv(o.getReceiptNumber() != null ? o.getReceiptNumber() : ("REC-" + o.getId()));
                String cName = escapeCsv(o.getUser() != null ? o.getUser().getUsername() : "Reader");
                String cEmail = escapeCsv(o.getUser() != null ? o.getUser().getEmail() : "");
                String bTitle = escapeCsv(o.getBook() != null ? o.getBook().getTitle() : "Edition");
                String bCat = escapeCsv(o.getBook() != null && o.getBook().getCategory() != null ? o.getBook().getCategory().getName() : "");
                int qty = o.getQuantity();
                double unitPrice = o.getUnitPrice();
                double total = o.getAmount();
                String status = escapeCsv(o.getPaymentStatus() != null ? o.getPaymentStatus() : "PAID");
                String date = escapeCsv(o.getOrderDate() != null ? o.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "");

                pw.printf("%s,%s,%s,%s,%s,%s,%d,%.2f,%.2f,%s,%s%n", ordNo, recNo, cName, cEmail, bTitle, bCat, qty, unitPrice, total, status, date);
            }

            pw.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate CSV export: " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}

