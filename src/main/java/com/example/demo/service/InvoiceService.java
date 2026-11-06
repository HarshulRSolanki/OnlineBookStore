package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.Order;
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
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

@Service
public class InvoiceService {

    private static final DeviceRgb COLOR_PRIMARY = new DeviceRgb(44, 33, 27);      // #2C211B Espresso
    private static final DeviceRgb COLOR_ACCENT = new DeviceRgb(139, 107, 69);     // #8B6B45 Gold/Tan
    private static final DeviceRgb COLOR_MUTED = new DeviceRgb(120, 115, 110);     // Muted Gray
    private static final DeviceRgb COLOR_BG_LIGHT = new DeviceRgb(250, 247, 242);  // Cream #FAF7F2
    private static final DeviceRgb COLOR_BORDER = new DeviceRgb(229, 217, 200);    // #E5D9C8
    private static final DeviceRgb COLOR_SUCCESS = new DeviceRgb(46, 125, 50);     // Deep Green

    public byte[] generatePdfInvoice(Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            pdfDoc.setDefaultPageSize(PageSize.A4);

            Document document = new Document(pdfDoc);
            document.setMargins(36, 40, 36, 40);

            // 1. HEADER TABLE (Brand on Left, Invoice Details on Right)
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
            headerTable.setWidth(UnitValue.createPercentValue(100));

            // Brand cell
            Cell brandCell = new Cell().setBorder(Border.NO_BORDER);
            brandCell.add(new Paragraph("QUICKREAD")
                    .setFontSize(22)
                    .setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setMarginBottom(0));
            brandCell.add(new Paragraph("The Reader's Atelier & Rare Book Curation")
                    .setFontSize(9)
                    .setFontColor(COLOR_ACCENT)
                    .setMarginBottom(4));
            brandCell.add(new Paragraph("Official Digital Receipt & Proof of Purchase")
                    .setFontSize(8)
                    .setFontColor(COLOR_MUTED));
            headerTable.addCell(brandCell);

            // Invoice details cell
            Cell invoiceMetaCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            invoiceMetaCell.add(new Paragraph("TAX INVOICE")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setMarginBottom(4));
            invoiceMetaCell.add(new Paragraph("Receipt #: " + order.getReceiptNumber())
                    .setFontSize(9).setBold().setFontColor(COLOR_PRIMARY));
            invoiceMetaCell.add(new Paragraph("Order Ref: " + order.getOrderNumber())
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            String dateFormatted = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))
                    : "N/A";
            invoiceMetaCell.add(new Paragraph("Date: " + dateFormatted)
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            invoiceMetaCell.add(new Paragraph("Status: PAID - VERIFIED")
                    .setFontSize(9).setBold().setFontColor(COLOR_SUCCESS));
            headerTable.addCell(invoiceMetaCell);

            document.add(headerTable);
            document.add(new Paragraph("\n").setFontSize(6));

            // 2. CUSTOMER & TRANSACTION DETAILS TABLE
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{50, 50}));
            detailsTable.setWidth(UnitValue.createPercentValue(100));
            detailsTable.setBackgroundColor(COLOR_BG_LIGHT);
            detailsTable.setBorder(new SolidBorder(COLOR_BORDER, 1));
            detailsTable.setPadding(10);

            Cell billedTo = new Cell().setBorder(Border.NO_BORDER).setPadding(10);
            billedTo.add(new Paragraph("BILLED TO / READER DOSSIER")
                    .setFontSize(8).setBold().setFontColor(COLOR_ACCENT).setMarginBottom(4));
            String customerName = order.getUser() != null ? order.getUser().getUsername() : "Valued Reader";
            String customerEmail = order.getUser() != null ? order.getUser().getEmail() : "N/A";
            billedTo.add(new Paragraph(customerName).setFontSize(11).setBold().setFontColor(COLOR_PRIMARY));
            billedTo.add(new Paragraph(customerEmail).setFontSize(9).setFontColor(COLOR_MUTED));
            detailsTable.addCell(billedTo);

            Cell paymentInfo = new Cell().setBorder(Border.NO_BORDER).setPadding(10);
            paymentInfo.add(new Paragraph("PAYMENT METHOD & CLEARING")
                    .setFontSize(8).setBold().setFontColor(COLOR_ACCENT).setMarginBottom(4));
            paymentInfo.add(new Paragraph("Stripe Secure Payment Gateway").setFontSize(10).setBold().setFontColor(COLOR_PRIMARY));
            String stripeId = order.getStripeSessionId() != null ? order.getStripeSessionId() : "Stripe-Verified";
            if (stripeId.length() > 28) {
                stripeId = stripeId.substring(0, 28) + "...";
            }
            paymentInfo.add(new Paragraph("Ref: " + stripeId).setFontSize(8).setFontColor(COLOR_MUTED));
            paymentInfo.add(new Paragraph("Delivery: Instant Digital Library Download (PDF)").setFontSize(8).setFontColor(COLOR_MUTED));
            detailsTable.addCell(paymentInfo);

            document.add(detailsTable);
            document.add(new Paragraph("\n").setFontSize(8));

            // 3. ITEM TABLE
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{10, 45, 15, 15, 15}));
            itemTable.setWidth(UnitValue.createPercentValue(100));

            // Table Header
            String[] headers = {"#", "Book Description", "Qty", "Unit Price", "Total (INR)"};
            for (String h : headers) {
                Cell hCell = new Cell().setBackgroundColor(COLOR_PRIMARY).setPadding(8);
                hCell.add(new Paragraph(h).setFontSize(9).setBold().setFontColor(ColorConstants.WHITE));
                if (h.contains("Price") || h.contains("Total") || h.equals("Qty")) {
                    hCell.setTextAlignment(TextAlignment.RIGHT);
                }
                itemTable.addHeaderCell(hCell);
            }

            // Book Row
            String bookTitle = order.getBook() != null ? order.getBook().getTitle() : "Curated Literary Edition";
            String bookAuthor = order.getBook() != null ? order.getBook().getAuthor() : "Unknown Author";
            String bookCategory = order.getBook() != null && order.getBook().getCategory() != null ? order.getBook().getCategory().getName() : "General";

            Cell col1 = new Cell().setPadding(8).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
            col1.add(new Paragraph("1").setFontSize(9).setFontColor(COLOR_PRIMARY));
            itemTable.addCell(col1);

            Cell col2 = new Cell().setPadding(8).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
            col2.add(new Paragraph(bookTitle).setFontSize(10).setBold().setFontColor(COLOR_PRIMARY));
            col2.add(new Paragraph("By " + bookAuthor + " • Category: " + bookCategory).setFontSize(8).setFontColor(COLOR_MUTED));
            itemTable.addCell(col2);

            Cell col3 = new Cell().setPadding(8).setTextAlignment(TextAlignment.RIGHT).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
            col3.add(new Paragraph(String.valueOf(order.getQuantity())).setFontSize(9).setFontColor(COLOR_PRIMARY));
            itemTable.addCell(col3);

            Cell col4 = new Cell().setPadding(8).setTextAlignment(TextAlignment.RIGHT).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
            col4.add(new Paragraph(String.format("₹%.2f", order.getUnitPrice())).setFontSize(9).setFontColor(COLOR_PRIMARY));
            itemTable.addCell(col4);

            Cell col5 = new Cell().setPadding(8).setTextAlignment(TextAlignment.RIGHT).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
            col5.add(new Paragraph(String.format("₹%.2f", order.getAmount())).setFontSize(9).setBold().setFontColor(COLOR_PRIMARY));
            itemTable.addCell(col5);

            document.add(itemTable);
            document.add(new Paragraph("\n").setFontSize(6));

            // 4. SUMMARY CALCULATION TABLE
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));

            // Left note
            Cell noteCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
            noteCell.add(new Paragraph("Digital Rights & License:").setFontSize(8).setBold().setFontColor(COLOR_ACCENT));
            noteCell.add(new Paragraph("This eBook is registered to your reader account with lifetime personal reading access. You can download and read across any e-reader, tablet, or desktop.")
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            summaryTable.addCell(noteCell);

            // Right calculations
            Cell calcCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
            Table innerCalc = new Table(UnitValue.createPercentArray(new float[]{60, 40}));
            innerCalc.setWidth(UnitValue.createPercentValue(100));

            addCalcRow(innerCalc, "Subtotal:", String.format("₹%.2f", order.getSubtotal()), false);
            addCalcRow(innerCalc, "Digital Delivery:", "FREE (0.00)", false);
            if (order.getTax() > 0) {
                addCalcRow(innerCalc, "Taxes:", String.format("₹%.2f", order.getTax()), false);
            }
            if (order.getDiscount() > 0) {
                addCalcRow(innerCalc, "Discount:", String.format("-₹%.2f", order.getDiscount()), false);
            }
            addCalcRow(innerCalc, "Total Paid:", String.format("₹%.2f", order.getAmount()), true);

            calcCell.add(innerCalc);
            summaryTable.addCell(calcCell);

            document.add(summaryTable);
            document.add(new Paragraph("\n").setFontSize(12));

            // 5. FOOTER & SUPPORT
            Table footerTable = new Table(UnitValue.createPercentArray(new float[]{100}));
            footerTable.setWidth(UnitValue.createPercentValue(100));
            Cell footerCell = new Cell().setBorderTop(new SolidBorder(COLOR_BORDER, 1))
                    .setBorderBottom(Border.NO_BORDER)
                    .setBorderLeft(Border.NO_BORDER)
                    .setBorderRight(Border.NO_BORDER)
                    .setPaddingTop(12)
                    .setTextAlignment(TextAlignment.CENTER);
            footerCell.add(new Paragraph("Thank you for supporting authors and literature at Quickread.")
                    .setFontSize(9).setBold().setFontColor(COLOR_PRIMARY));
            footerCell.add(new Paragraph("Questions or concierge support? Contact us at concierge@quickread.com • http://localhost:8525")
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            footerCell.add(new Paragraph("© " + java.time.Year.now().getValue() + " Quickread Bookstore. All Rights Reserved. Computer Generated Invoice.")
                    .setFontSize(7).setFontColor(COLOR_MUTED));
            footerTable.addCell(footerCell);

            document.add(footerTable);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF invoice: " + e.getMessage(), e);
        }
    }

    private void addCalcRow(Table table, String label, String value, boolean isTotal) {
        Cell labelCell = new Cell().setBorder(Border.NO_BORDER).setPadding(3);
        Cell valCell = new Cell().setBorder(Border.NO_BORDER).setPadding(3).setTextAlignment(TextAlignment.RIGHT);

        Paragraph p1 = new Paragraph(label).setFontSize(isTotal ? 11 : 9);
        Paragraph p2 = new Paragraph(value).setFontSize(isTotal ? 12 : 9);

        if (isTotal) {
            p1.setBold().setFontColor(COLOR_PRIMARY);
            p2.setBold().setFontColor(COLOR_PRIMARY);
            labelCell.setBorderTop(new SolidBorder(COLOR_PRIMARY, 1));
            valCell.setBorderTop(new SolidBorder(COLOR_PRIMARY, 1));
        } else {
            p1.setFontColor(COLOR_MUTED);
            p2.setFontColor(COLOR_PRIMARY);
        }

        labelCell.add(p1);
        valCell.add(p2);
        table.addCell(labelCell);
        table.addCell(valCell);
    }
}
