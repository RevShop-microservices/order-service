package com.example.order_service.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.example.order_service.models.Order;
import com.example.order_service.models.OrderItem;

import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfInvoiceService {

    public byte[] generateInvoicePdf(Order order) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Brand Fonts
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 26, new Color(22, 163, 74));
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            // --- HEADER ---
            Paragraph brand = new Paragraph("NexShop", brandFont);
            brand.setAlignment(Element.ALIGN_LEFT);
            document.add(brand);

            Paragraph subtitle = new Paragraph("AI-Enhanced E-Commerce Marketplace", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY));
            subtitle.setSpacingAfter(20);
            document.add(subtitle);

            // Line Separator
            PdfPTable hrTable = new PdfPTable(1);
            hrTable.setWidthPercentage(100);
            PdfPCell hrCell = new PdfPCell();
            hrCell.setBorder(Rectangle.BOTTOM);
            hrCell.setBorderWidth(1f);
            hrCell.setBorderColor(new Color(229, 231, 235));
            hrCell.setFixedHeight(1);
            hrTable.addCell(hrCell);
            document.add(hrTable);

            // --- INVOICE TITLE & META INFO ---
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setSpacingBefore(15);
            metaTable.setSpacingAfter(20);

            PdfPCell toCell = new PdfPCell();
            toCell.setBorder(Rectangle.NO_BORDER);
            toCell.addElement(new Paragraph("INVOICE TO:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.GRAY)));
            toCell.addElement(new Paragraph("Customer ID: " + order.getUserId(), boldFont));
            toCell.addElement(new Paragraph("Shipping Address ID: " + order.getAddressId(), normalFont));
            metaTable.addCell(toCell);

            PdfPCell infoCell = new PdfPCell();
            infoCell.setBorder(Rectangle.NO_BORDER);
            infoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph invTitle = new Paragraph("TAX INVOICE", titleFont);
            invTitle.setAlignment(Element.ALIGN_RIGHT);
            infoCell.addElement(invTitle);

            String orderDateStr = order.getOrderDate() != null
                    ? order.getOrderDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "N/A";
            Paragraph oId = new Paragraph("Order ID: #" + order.getOrderId(), boldFont);
            oId.setAlignment(Element.ALIGN_RIGHT);
            infoCell.addElement(oId);

            Paragraph oDate = new Paragraph("Date: " + orderDateStr, normalFont);
            oDate.setAlignment(Element.ALIGN_RIGHT);
            infoCell.addElement(oDate);

            metaTable.addCell(infoCell);
            document.add(metaTable);

            // --- PRODUCTS TABLE ---
            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{45f, 15f, 15f, 25f});
            table.setSpacingAfter(20);

            String[] headers = {"Product Name", "Unit Price", "Qty", "Total Price"};
            for (String header : headers) {
                PdfPCell headerCell = new PdfPCell(new Phrase(header, tableHeaderFont));
                headerCell.setBackgroundColor(new Color(22, 163, 74));
                headerCell.setBorderColor(new Color(21, 128, 61));
                headerCell.setPadding(8);
                headerCell.setHorizontalAlignment(header.equals("Product Name") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
                table.addCell(headerCell);
            }

            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    PdfPCell cellName = new PdfPCell(new Phrase(item.getProductName(), normalFont));
                    cellName.setPadding(8);
                    cellName.setBorderColor(new Color(243, 244, 246));
                    cellName.setHorizontalAlignment(Element.ALIGN_LEFT);
                    table.addCell(cellName);

                    PdfPCell cellPrice = new PdfPCell(new Phrase("INR " + String.format("%.2f", item.getPrice()), normalFont));
                    cellPrice.setPadding(8);
                    cellPrice.setBorderColor(new Color(243, 244, 246));
                    cellPrice.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellPrice);

                    PdfPCell cellQty = new PdfPCell(new Phrase(String.valueOf(item.getQuantity()), normalFont));
                    cellQty.setPadding(8);
                    cellQty.setBorderColor(new Color(243, 244, 246));
                    cellQty.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellQty);

                    double total = item.getPrice() * item.getQuantity();
                    PdfPCell cellTotal = new PdfPCell(new Phrase("INR " + String.format("%.2f", total), normalFont));
                    cellTotal.setPadding(8);
                    cellTotal.setBorderColor(new Color(243, 244, 246));
                    cellTotal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(cellTotal);
                }
            }
            document.add(table);

            // --- SUMMARY TOTALS ---
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setWidths(new float[]{60f, 40f});
            summaryTable.setSpacingAfter(30);

            PdfPCell leftEmpty = new PdfPCell();
            leftEmpty.setBorder(Rectangle.NO_BORDER);
            summaryTable.addCell(leftEmpty);

            PdfPCell rightSum = new PdfPCell();
            rightSum.setBorder(Rectangle.NO_BORDER);

            PdfPTable sumGrid = new PdfPTable(2);
            sumGrid.setWidthPercentage(100);

            sumGrid.addCell(getNoBorderCell("Subtotal:", normalFont, Element.ALIGN_LEFT));
            sumGrid.addCell(getNoBorderCell("INR " + String.format("%.2f", order.getSubtotal()), normalFont, Element.ALIGN_RIGHT));

            if (order.getDiscount() != null && order.getDiscount() > 0) {
                sumGrid.addCell(getNoBorderCell("Discount Applied:", FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(22, 163, 74)), Element.ALIGN_LEFT));
                sumGrid.addCell(getNoBorderCell("- INR " + String.format("%.2f", order.getDiscount()), FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(22, 163, 74)), Element.ALIGN_RIGHT));
            }

            sumGrid.addCell(getNoBorderCell("Shipping:", normalFont, Element.ALIGN_LEFT));
            sumGrid.addCell(getNoBorderCell("FREE", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(22, 163, 74)), Element.ALIGN_RIGHT));

            PdfPCell lineCellLeft = new PdfPCell();
            lineCellLeft.setBorder(Rectangle.TOP);
            lineCellLeft.setBorderWidth(1f);
            lineCellLeft.setBorderColor(new Color(229, 231, 235));
            sumGrid.addCell(lineCellLeft);

            PdfPCell lineCellRight = new PdfPCell();
            lineCellRight.setBorder(Rectangle.TOP);
            lineCellRight.setBorderWidth(1f);
            lineCellRight.setBorderColor(new Color(229, 231, 235));
            sumGrid.addCell(lineCellRight);

            sumGrid.addCell(getNoBorderCell("Total Amount Paid:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.BLACK), Element.ALIGN_LEFT));
            sumGrid.addCell(getNoBorderCell("INR " + String.format("%.2f", order.getTotalAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new Color(22, 163, 74)), Element.ALIGN_RIGHT));

            rightSum.addElement(sumGrid);
            summaryTable.addCell(rightSum);
            document.add(summaryTable);

            // --- FOOTER ---
            Paragraph footer = new Paragraph("Thank you for shopping with NexShop!\nIf you have any questions regarding this invoice, feel free to contact us at support@nexshop.com.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(40);
            document.add(footer);

            document.close();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private PdfPCell getNoBorderCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4);
        return cell;
    }
}
