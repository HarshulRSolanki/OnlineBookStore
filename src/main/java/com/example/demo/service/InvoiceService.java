package com.example.demo.service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.Entity.Order;
import com.example.demo.Entity.OrderItem;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

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
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{58, 42}));
            headerTable.setWidth(UnitValue.createPercentValue(100));

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
            brandCell.add(new Paragraph("Official Combined Digital Receipt & Tax Invoice")
                    .setFontSize(8)
                    .setFontColor(COLOR_MUTED));
            headerTable.addCell(brandCell);

            Cell invoiceMetaCell = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
            invoiceMetaCell.add(new Paragraph("TAX INVOICE")
                    .setFontSize(16)
                    .setBold()
                    .setFontColor(COLOR_PRIMARY)
                    .setMarginBottom(4));
            invoiceMetaCell.add(new Paragraph("Receipt #: " + order.getReceiptNumber())
                    .setFontSize(9).setBold().setFontColor(COLOR_PRIMARY));
            invoiceMetaCell.add(new Paragraph("Order ID: " + order.getOrderNumber())
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            String dateFormatted = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))
                    : "N/A";
            invoiceMetaCell.add(new Paragraph("Date: " + dateFormatted)
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            String statusText = order.isPaid() ? "Status: PAID - VERIFIED" : ("Status: " + order.getPaymentStatus());
            invoiceMetaCell.add(new Paragraph(statusText)
                    .setFontSize(9).setBold().setFontColor(order.isPaid() ? COLOR_SUCCESS : COLOR_PRIMARY));
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
            if (stripeId.length() > 30) {
                stripeId = stripeId.substring(0, 30) + "...";
            }
            paymentInfo.add(new Paragraph("Ref: " + stripeId).setFontSize(8).setFontColor(COLOR_MUTED));
            paymentInfo.add(new Paragraph("Items in Order: " + order.getDistinctBookCount()
                    + " title(s), " + order.getTotalItemQuantity() + " copy/copies").setFontSize(8).setFontColor(COLOR_MUTED));
            paymentInfo.add(new Paragraph("Fulfillment: " + order.getDeliveryMethodDisplayName()
                    + " (" + order.getDisplayOrderStatus() + ")").setFontSize(8).setBold().setFontColor(COLOR_PRIMARY));
            detailsTable.addCell(paymentInfo);

            document.add(detailsTable);

            if (order.hasDeliveryDetails()) {
                document.add(new Paragraph("\n").setFontSize(4));
                Table deliveryTable = new Table(UnitValue.createPercentArray(new float[]{100}));
                deliveryTable.setWidth(UnitValue.createPercentValue(100));
                deliveryTable.setBackgroundColor(COLOR_BG_LIGHT);
                deliveryTable.setBorder(new SolidBorder(COLOR_BORDER, 1));
                Cell dCell = new Cell().setBorder(Border.NO_BORDER).setPadding(10);

                if (order.isHomeDelivery()) {
                    dCell.add(new Paragraph("HOME DELIVERY ADDRESS (SNAPSHOT)")
                            .setFontSize(8).setBold().setFontColor(COLOR_ACCENT).setMarginBottom(3));
                    String recipientLine = (order.getShippingFullName() != null ? order.getShippingFullName() : customerName)
                            + (order.getShippingAddressType() != null ? " [" + order.getShippingAddressType() + "]" : "")
                            + (order.getShippingMobile() != null ? " | Mobile: " + order.getShippingMobile() : "");
                    dCell.add(new Paragraph(recipientLine).setFontSize(9).setBold().setFontColor(COLOR_PRIMARY));
                    dCell.add(new Paragraph(order.getShippingAddressSnapshot() != null ? order.getShippingAddressSnapshot() : "")
                            .setFontSize(8).setFontColor(COLOR_MUTED));
                } else if (order.isStorePickup()) {
                    dCell.add(new Paragraph("PHYSICAL STORE PICKUP DETAILS")
                            .setFontSize(8).setBold().setFontColor(COLOR_ACCENT).setMarginBottom(3));
                    dCell.add(new Paragraph(order.getPickupStoreName() != null ? order.getPickupStoreName() : "Quickread Store")
                            .setFontSize(10).setBold().setFontColor(COLOR_PRIMARY));
                    dCell.add(new Paragraph(order.getFormattedStoreLocation()).setFontSize(8).setFontColor(COLOR_MUTED));
                    dCell.add(new Paragraph("Contact: " + (order.getPickupStoreContact() != null ? order.getPickupStoreContact() : "N/A")
                            + " | Hours: " + (order.getPickupStoreOpeningHours() != null ? order.getPickupStoreOpeningHours() : "Standard Hours"))
                            .setFontSize(8).setFontColor(COLOR_MUTED));
                    if (order.getPickupInstructions() != null && !order.getPickupInstructions().isBlank()) {
                        dCell.add(new Paragraph("Pickup Instructions: " + order.getPickupInstructions())
                                .setFontSize(8).setBold().setFontColor(COLOR_PRIMARY).setMarginTop(3));
                    }
                }
                deliveryTable.addCell(dCell);
                document.add(deliveryTable);
            }

            document.add(new Paragraph("\n").setFontSize(8));

            // 3. COMBINED MULTI-BOOK ITEM TABLE
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{6, 12, 42, 10, 15, 15}));
            itemTable.setWidth(UnitValue.createPercentValue(100));

            String[] headers = {"#", "Cover", "Book Description", "Qty", "Unit Price", "Subtotal"};
            for (String h : headers) {
                Cell hCell = new Cell().setBackgroundColor(COLOR_PRIMARY).setPadding(7);
                hCell.add(new Paragraph(h).setFontSize(9).setBold().setFontColor(ColorConstants.WHITE));
                if (h.equals("Unit Price") || h.equals("Subtotal") || h.equals("Qty")) {
                    hCell.setTextAlignment(TextAlignment.RIGHT);
                } else if (h.equals("Cover") || h.equals("#")) {
                    hCell.setTextAlignment(TextAlignment.CENTER);
                }
                itemTable.addHeaderCell(hCell);
            }

            List<OrderItem> effectiveItems = order.getEffectiveItems();
            int index = 1;
            if (effectiveItems.isEmpty()) {
                Cell emptyCell = new Cell(1, 6).setPadding(10).setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                emptyCell.add(new Paragraph("Curated Literary Edition").setFontSize(9).setFontColor(COLOR_PRIMARY));
                itemTable.addCell(emptyCell);
            } else {
                for (OrderItem item : effectiveItems) {
                    Cell colIdx = new Cell().setPadding(7).setTextAlignment(TextAlignment.CENTER)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                    colIdx.add(new Paragraph(String.valueOf(index++)).setFontSize(9).setFontColor(COLOR_PRIMARY));
                    itemTable.addCell(colIdx);

                    Cell colCover = new Cell().setPadding(5).setTextAlignment(TextAlignment.CENTER)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                    boolean imageAdded = false;
                    if (item.getBook() != null && item.getBook().getCoverImage() != null && item.getBook().getCoverImage().length > 0) {
                        try {
                            Image img = new Image(ImageDataFactory.create(item.getBook().getCoverImage()));
                            img.setWidth(28).setAutoScaleHeight(true);
                            colCover.add(img);
                            imageAdded = true;
                        } catch (Exception ignored) {
                            imageAdded = false;
                        }
                    }
                    if (!imageAdded) {
                        colCover.add(new Paragraph("BOOK").setFontSize(7).setBold().setFontColor(COLOR_ACCENT));
                    }
                    itemTable.addCell(colCover);

                    Cell colDesc = new Cell().setPadding(7).setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                    colDesc.add(new Paragraph(item.getEffectiveTitle()).setFontSize(10).setBold().setFontColor(COLOR_PRIMARY));
                    colDesc.add(new Paragraph("By " + item.getEffectiveAuthor() + " | Category: " + item.getEffectiveCategory())
                            .setFontSize(8).setFontColor(COLOR_MUTED));
                    itemTable.addCell(colDesc);

                    Cell colQty = new Cell().setPadding(7).setTextAlignment(TextAlignment.RIGHT)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                    colQty.add(new Paragraph(String.valueOf(item.getQuantity())).setFontSize(9).setFontColor(COLOR_PRIMARY));
                    itemTable.addCell(colQty);

                    Cell colUnit = new Cell().setPadding(7).setTextAlignment(TextAlignment.RIGHT)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                    colUnit.add(new Paragraph(String.format("INR %.2f", item.getUnitPrice())).setFontSize(9).setFontColor(COLOR_PRIMARY));
                    itemTable.addCell(colUnit);

                    Cell colSub = new Cell().setPadding(7).setTextAlignment(TextAlignment.RIGHT)
                            .setVerticalAlignment(VerticalAlignment.MIDDLE)
                            .setBorder(new SolidBorder(COLOR_BORDER, 0.5f));
                    colSub.add(new Paragraph(String.format("INR %.2f", item.getSubtotal())).setFontSize(9).setBold().setFontColor(COLOR_PRIMARY));
                    itemTable.addCell(colSub);
                }
            }

            document.add(itemTable);
            document.add(new Paragraph("\n").setFontSize(6));

            // 4. SUMMARY CALCULATION TABLE
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{55, 45}));
            summaryTable.setWidth(UnitValue.createPercentValue(100));

            Cell noteCell = new Cell().setBorder(Border.NO_BORDER).setPadding(8);
            noteCell.add(new Paragraph("Digital Rights & License:").setFontSize(8).setBold().setFontColor(COLOR_ACCENT));
            noteCell.add(new Paragraph("All acquired digital editions listed in this receipt are registered to your reader account with lifetime personal reading access across e-reader, tablet, and desktop.")
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            summaryTable.addCell(noteCell);

            Cell calcCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
            Table innerCalc = new Table(UnitValue.createPercentArray(new float[]{58, 42}));
            innerCalc.setWidth(UnitValue.createPercentValue(100));

            addCalcRow(innerCalc, "Subtotal:", String.format("INR %.2f", order.getSubtotal()), false);
            addCalcRow(innerCalc, "Delivery Charges:",
                    order.getDeliveryCharge() > 0 ? String.format("INR %.2f", order.getDeliveryCharge()) : "FREE (INR 0.00)", false);
            addCalcRow(innerCalc, "Taxes:", String.format("INR %.2f", order.getTax()), false);
            addCalcRow(innerCalc, "Discount:",
                    order.getDiscount() > 0 ? String.format("-INR %.2f", order.getDiscount()) : "INR 0.00", false);
            addCalcRow(innerCalc, "Total Paid:", String.format("INR %.2f", order.getAmount()), true);

            calcCell.add(innerCalc);
            summaryTable.addCell(calcCell);

            document.add(summaryTable);
            document.add(new Paragraph("\n").setFontSize(10));

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
            footerCell.add(new Paragraph("Questions or concierge support? Contact us at concierge@quickread.com")
                    .setFontSize(8).setFontColor(COLOR_MUTED));
            footerCell.add(new Paragraph("(c) " + java.time.Year.now().getValue() + " Quickread Bookstore. All Rights Reserved. Computer Generated Invoice.")
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
        Paragraph p2 = new Paragraph(value).setFontSize(isTotal ? 11 : 9);

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
