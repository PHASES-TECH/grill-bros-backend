package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.OrderItem;
import com.grill_bros.backend.model.OrderItemModifier;
import com.grill_bros.backend.repository.OrderRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.grill_bros.backend.model.Receipt;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final OrderRepository orderRepository;

    private static final BaseColor BRAND_ORANGE  = new BaseColor(0xE8, 0x42, 0x0E); // #E8420E
    private static final BaseColor BRAND_DARK    = new BaseColor(0x1A, 0x1A, 0x1A); // near-black
    private static final BaseColor BRAND_LIGHT   = new BaseColor(0xF7, 0xF7, 0xF7); // off-white row
    private static final BaseColor BRAND_WHITE   = BaseColor.WHITE;
    private static final BaseColor TEXT_MUTED    = new BaseColor(0x6B, 0x72, 0x80); // gray

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_BRAND_TITLE;
    private static final Font FONT_BRAND_SUB;
    private static final Font FONT_SECTION_HEADER;
    private static final Font FONT_BODY;
    private static final Font FONT_BODY_BOLD;
    private static final Font FONT_SMALL;
    private static final Font FONT_SMALL_MUTED;
    private static final Font FONT_TABLE_HEADER;
    private static final Font FONT_TABLE_CELL;
    private static final Font FONT_TOTAL;
    private static final Font FONT_FOOTER;

    static {
        FONT_BRAND_TITLE   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   28, BRAND_ORANGE);
        FONT_BRAND_SUB     = FontFactory.getFont(FontFactory.HELVETICA,         11, TEXT_MUTED);
        FONT_SECTION_HEADER= FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10, BRAND_WHITE);
        FONT_BODY          = FontFactory.getFont(FontFactory.HELVETICA,         10, BRAND_DARK);
        FONT_BODY_BOLD     = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   10, BRAND_DARK);
        FONT_SMALL         = FontFactory.getFont(FontFactory.HELVETICA,          9, BRAND_DARK);
        FONT_SMALL_MUTED   = FontFactory.getFont(FontFactory.HELVETICA,          8, TEXT_MUTED);
        FONT_TABLE_HEADER  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,    9, BRAND_WHITE);
        FONT_TABLE_CELL    = FontFactory.getFont(FontFactory.HELVETICA,          9, BRAND_DARK);
        FONT_TOTAL         = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   13, BRAND_ORANGE);
        FONT_FOOTER        = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, TEXT_MUTED);
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                    .withZone(ZoneId.of("Africa/Accra"));

    public byte[] generateReceiptPdf(Receipt receipt) {

        Order order = orderRepository.findByIdWithItems(receipt.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        Document document = new Document();

        try {
            Rectangle pageSize = new Rectangle(PageSize.A5);
            Document doc = new Document(pageSize, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, out);

            // Add page border event
            writer.setPageEvent(new PageBorderEvent());

            doc.open();

            addHeader(doc, receipt);
            addDivider(doc);
            addOrderMeta(doc, receipt, order);
            addDivider(doc);
            addItemsTable(doc, receipt, order);
            addTotalRow(doc, receipt);
            addDivider(doc);
            addFooter(doc, receipt);

            doc.close();

//            document.add(new Paragraph("RECEIPT"));
//            document.add(new Paragraph("Reference: " + receipt.getReference()));
//            document.add(new Paragraph("Amount: GHS " + receipt.getAmount()));
//            document.add(new Paragraph("Customer: " + receipt.getCustomerName()));
//            document.add(new Paragraph("Date: " + receipt.getIssuedAt()));
//
//            document.close();

        } catch (Exception e) {
            log.error("Failed to generate PDF for receipt {}: {}", receipt.getReference(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF", e); // preserve cause
        }

        return out.toByteArray();
    }

    private void addHeader(Document doc, Receipt receipt) throws DocumentException {
        // Brand name
        Paragraph brand = new Paragraph("The GrillBros", FONT_BRAND_TITLE);
        brand.setAlignment(Element.ALIGN_CENTER);
        brand.setSpacingAfter(2);
        doc.add(brand);

        // Tagline
        Paragraph tagline = new Paragraph("Flame-Grilled. Always Fresh.", FONT_BRAND_SUB);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingAfter(10);
        doc.add(tagline);

        // "PAYMENT RECEIPT" badge — orange box
        PdfPTable badge = new PdfPTable(1);
        badge.setWidthPercentage(60);
        badge.setHorizontalAlignment(Element.ALIGN_CENTER);

        PdfPCell badgeCell = new PdfPCell(new Phrase("PAYMENT RECEIPT", FONT_SECTION_HEADER));
        badgeCell.setBackgroundColor(BRAND_ORANGE);
        badgeCell.setBorder(Rectangle.NO_BORDER);
        badgeCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        badgeCell.setPadding(6);
        badge.addCell(badgeCell);

        doc.add(badge);
        doc.add(Chunk.NEWLINE);
    }

    private void addOrderMeta(Document doc, Receipt receipt, Order order) throws DocumentException {
        PdfPTable meta = new PdfPTable(new float[]{40f, 60f});
        meta.setWidthPercentage(100);
        meta.setSpacingBefore(4);
        meta.setSpacingAfter(4);

        addMetaRow(meta, "Reference",    receipt.getReference());
        addMetaRow(meta, "Order No.",    order.getOrderNumber());
        addMetaRow(meta, "Customer",     receipt.getCustomerName());
        addMetaRow(meta, "Phone",        receipt.getCustomerPhone());
        addMetaRow(meta, "Date",         DATE_FMT.format(receipt.getIssuedAt()));

        doc.add(meta);
    }

    private void addMetaRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, FONT_SMALL_MUTED));
        labelCell.setBorder(Rectangle.NO_BORDER);
        labelCell.setPaddingBottom(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value != null ? value : "—", FONT_BODY_BOLD));
        valueCell.setBorder(Rectangle.NO_BORDER);
        valueCell.setPaddingBottom(5);

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    /** Line items table with alternating row shading */
    private void addItemsTable(Document doc, Receipt receipt, Order order) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{44f, 10f, 20f, 26f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(0);

        // Header row
        String[] headers = {"Item", "Qty", "Unit Price", "Total"};
        int[]    aligns  = {
                Element.ALIGN_LEFT, Element.ALIGN_CENTER,
                Element.ALIGN_RIGHT, Element.ALIGN_RIGHT
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell h = new PdfPCell(new Phrase(headers[i], FONT_TABLE_HEADER));
            h.setBackgroundColor(BRAND_DARK);
            h.setBorder(Rectangle.NO_BORDER);
            h.setHorizontalAlignment(aligns[i]);
            h.setPadding(6);
            table.addCell(h);
        }

        // Data rows — alternate background
        boolean alt = false;
        log.info("Order items pdf: {}", order.getItems());
        for (OrderItem item : order.getItems()) {
            BaseColor bg = alt ? BRAND_LIGHT : BRAND_WHITE;
            alt = !alt;

            Set<OrderItemModifier> modifiers = item.getModifiers();
            log.info("MODIFIERS: {}", modifiers);

            addItemCell(table, item.getItemName(),              bg, Element.ALIGN_LEFT);
            addItemCell(table, String.valueOf(item.getQuantity()), bg, Element.ALIGN_CENTER);
            addItemCell(table, "GHS " + fmt(item.getUnitPrice()), bg, Element.ALIGN_RIGHT);
            addItemCell(table, "GHS " + fmt(item.getLineTotal()), bg, Element.ALIGN_RIGHT);

            for (OrderItemModifier modifier : modifiers) {
                addItemCell(table, modifier.getModifier().getName(),              bg, Element.ALIGN_LEFT);
                addItemCell(table, "GHS " + fmt(modifier.getModifier().getPrice()), bg, Element.ALIGN_RIGHT);
            }
        }

        doc.add(table);
    }

    private void addItemCell(PdfPTable table, String text,
                             BaseColor bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_TABLE_CELL));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(0xE5, 0xE7, 0xEB));
        cell.setBorderWidth(0.4f);
        cell.setHorizontalAlignment(align);
        cell.setPaddingTop(5);
        cell.setPaddingBottom(5);
        cell.setPaddingLeft(4);
        cell.setPaddingRight(4);
        table.addCell(cell);
    }

    /** Right-aligned total line with orange amount */
    private void addTotalRow(Document doc, Receipt receipt) throws DocumentException {
        PdfPTable total = new PdfPTable(new float[]{60f, 40f});
        total.setWidthPercentage(100);
        total.setSpacingBefore(0);
        total.setSpacingAfter(8);

        PdfPCell emptyCell = new PdfPCell(new Phrase(""));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        emptyCell.setBackgroundColor(BRAND_DARK);
        emptyCell.setPadding(7);
        total.addCell(emptyCell);

        PdfPCell totalCell = new PdfPCell(
                new Phrase("TOTAL  GHS " + fmt(receipt.getAmount()), FONT_TOTAL));
        totalCell.setBackgroundColor(BRAND_DARK);
        totalCell.setBorder(Rectangle.NO_BORDER);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalCell.setPadding(7);
        total.addCell(totalCell);

        doc.add(total);
    }

    /** Thank-you note and legal footer */
    private void addFooter(Document doc, Receipt receipt) throws DocumentException {
        Paragraph thanks = new Paragraph(
                "Thank you for dining with GrillBros, " + receipt.getCustomerName() + "!\n" +
                        "We hope to see you again soon. 🔥",
                FONT_BODY);
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingBefore(6);
        thanks.setSpacingAfter(12);
        doc.add(thanks);

        Paragraph footer = new Paragraph(
                "GrillBros · support@grillbros.com · +233 XX XXX XXXX\n" +
                        "This is an automatically generated receipt. Please retain for your records.",
                FONT_FOOTER);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    private void addDivider(Document doc) throws DocumentException {
        LineSeparator line = new LineSeparator(0.5f, 100, TEXT_MUTED, Element.ALIGN_CENTER, -2);
        doc.add(new Chunk(line));
        doc.add(Chunk.NEWLINE);
    }

    private String fmt(BigDecimal value) {
        return value != null ? String.format("%,.2f", value) : "0.00";
    }

    private static class PageBorderEvent extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            cb.setColorStroke(BRAND_ORANGE);
            cb.setLineWidth(1.5f);
            cb.rectangle(
                    document.left()   - 18,
                    document.bottom() - 18,
                    document.right()  - document.left() + 36,
                    document.top()    - document.bottom() + 36
            );
            cb.stroke();
        }
    }
}