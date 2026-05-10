package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.OrderItem;
import com.grill_bros.backend.model.Receipt;
import com.grill_bros.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ReceiptEmailTemplate {

    private ReceiptEmailTemplate() {}
    // Brand colours
    private static final String ORANGE      = "#EF4444";
    private static final String DARK        = "#1A1A1A";
    private static final String LIGHT_BG    = "#F7F7F7";
    private static final String WHITE       = "#FFFFFF";
    private static final String MUTED       = "#6B7280";
    private static final String BORDER      = "#E5E7EB";
    private static final String ALT_ROW     = "#FFF4F1";

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm 'GMT'")
                    .withZone(ZoneId.of("Africa/Accra"));

    static String build(Receipt receipt, Order order) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>The GrillBros Receipt</title>
            </head>
            <body style="margin:0;padding:0;background-color:%s;font-family:Arial,Helvetica,sans-serif;">
            """.formatted(LIGHT_BG));

        // ── Outer wrapper ──────────────────────────────────────────────────
        sb.append("""
            <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                   style="background-color:%s;">
              <tr><td align="center" style="padding:32px 16px;">
            """.formatted(LIGHT_BG));

        // ── Email card ─────────────────────────────────────────────────────
        sb.append("""
            <table width="560" cellpadding="0" cellspacing="0" border="0"
                   style="background-color:%s;border-radius:8px;
                          box-shadow:0 2px 8px rgba(0,0,0,0.08);
                          overflow:hidden;max-width:100%%;">
            """.formatted(WHITE));

        // ── Orange top bar ─────────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="background-color:%s;padding:0;height:6px;"></td>
            </tr>
            """.formatted(ORANGE));

        // ── Header — brand name + tagline ──────────────────────────────────
        sb.append("""
            <tr>
              <td align="center" style="padding:36px 40px 20px;">
                <div style="font-size:32px;font-weight:900;color:%s;
                            letter-spacing:-1px;line-height:1;">GrillBros</div>
                <div style="font-size:12px;color:%s;margin-top:4px;
                            letter-spacing:1px;text-transform:uppercase;">
                  Flame-Grilled. Always Fresh.
                </div>
                <div style="display:inline-block;background-color:%s;
                            color:%s;font-size:11px;font-weight:700;
                            letter-spacing:1.5px;text-transform:uppercase;
                            padding:6px 18px;border-radius:20px;margin-top:16px;">
                  Payment Receipt
                </div>
              </td>
            </tr>
            """.formatted(ORANGE, MUTED, ORANGE, WHITE));

        // ── Greeting ───────────────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="padding:0 40px 16px;">
                <p style="margin:0;font-size:15px;color:%s;line-height:1.6;">
                  Hi <strong>%s</strong>,
                </p>
                <p style="margin:8px 0 0;font-size:14px;color:%s;line-height:1.6;">
                  Your payment was received successfully. Here's your receipt —
                  we've also attached a PDF copy for your records.
                </p>
              </td>
            </tr>
            """.formatted(DARK, esc(receipt.getCustomerName()), MUTED));

        // ── Order metadata box ─────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="padding:0 40px 24px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="background-color:%s;border-radius:6px;
                              border:1px solid %s;">
            """.formatted(LIGHT_BG, BORDER));

        addMetaRow(sb, "Reference",  receipt.getReference(),  true);
        addMetaRow(sb, "Order No.",  order.getOrderNumber(), false);
        addMetaRow(sb, "Phone",      receipt.getCustomerPhone(), true);
        addMetaRow(sb, "Date",       DATE_FMT.format(receipt.getIssuedAt()), false);

        sb.append("""
                </table>
              </td>
            </tr>
            """);

        // ── Items table ────────────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="padding:0 40px 8px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="border-radius:6px;overflow:hidden;
                              border:1px solid %s;">
            """.formatted(BORDER));

        // Table header
        sb.append("""
              <tr style="background-color:%s;">
                <th align="left"   style="padding:10px 12px;font-size:11px;
                                          color:%s;font-weight:700;
                                          text-transform:uppercase;letter-spacing:0.5px;
                                          border-bottom:1px solid %s;">Item</th>
                <th align="center" style="padding:10px 8px;font-size:11px;
                                          color:%s;font-weight:700;
                                          text-transform:uppercase;letter-spacing:0.5px;
                                          border-bottom:1px solid %s;">Qty</th>
                <th align="right"  style="padding:10px 8px;font-size:11px;
                                          color:%s;font-weight:700;
                                          text-transform:uppercase;letter-spacing:0.5px;
                                          border-bottom:1px solid %s;">Unit Price</th>
                <th align="right"  style="padding:10px 12px;font-size:11px;
                                          color:%s;font-weight:700;
                                          text-transform:uppercase;letter-spacing:0.5px;
                                          border-bottom:1px solid %s;">Total</th>
              </tr>
            """.formatted(
                DARK,
                WHITE, BORDER,
                WHITE, BORDER,
                WHITE, BORDER,
                WHITE, BORDER));

        // Item rows
        boolean alt = false;
        for (OrderItem item : order.getItems()) {
            String rowBg = alt ? ALT_ROW : WHITE;
            alt = !alt;
            sb.append("""
                <tr style="background-color:%s;">
                  <td style="padding:9px 12px;font-size:13px;color:%s;
                              border-bottom:1px solid %s;">%s</td>
                  <td align="center"
                      style="padding:9px 8px;font-size:13px;color:%s;
                             border-bottom:1px solid %s;">%d</td>
                  <td align="right"
                      style="padding:9px 8px;font-size:13px;color:%s;
                             border-bottom:1px solid %s;">GHS %s</td>
                  <td align="right"
                      style="padding:9px 12px;font-size:13px;color:%s;
                             font-weight:600;border-bottom:1px solid %s;">
                      GHS %s</td>
                </tr>
                """.formatted(
                    rowBg,
                    DARK, BORDER, esc(item.getItemName()),
                    MUTED, BORDER, item.getQuantity(),
                    MUTED, BORDER, fmt(item.getUnitPrice()),
                    DARK, BORDER, fmt(item.getLineTotal())));
        }

        // Total row
        sb.append("""
              <tr style="background-color:%s;">
                <td colspan="3" style="padding:12px 8px;font-size:13px;
                                        color:%s;font-weight:700;
                                        text-transform:uppercase;
                                        letter-spacing:0.5px;">
                  Total Amount
                </td>
                <td align="right"
                    style="padding:12px 12px;font-size:16px;
                           color:%s;font-weight:900;">
                  GHS %s
                </td>
              </tr>
            """.formatted(LIGHT_BG, MUTED, ORANGE, fmt(receipt.getAmount())));

        sb.append("""
                </table>
              </td>
            </tr>
            """);

        // ── Thank you callout ──────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="padding:24px 40px;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="background-color:%s;border-left:4px solid %s;
                              border-radius:0 6px 6px 0;padding:0;">
                  <tr>
                    <td style="padding:16px 20px;">
                      <p style="margin:0;font-size:15px;color:%s;font-weight:700;">
                        🔥 Thanks for choosing GrillBros!
                      </p>
                      <p style="margin:6px 0 0;font-size:13px;color:%s;line-height:1.6;">
                        We hope you enjoy every bite. Share your experience with us —
                        tag us on social or leave a review. See you next time!
                      </p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
            """.formatted(ALT_ROW, ORANGE, DARK, MUTED));

        // ── Footer ─────────────────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="background-color:%s;padding:20px 40px;
                         border-top:1px solid %s;">
                <p style="margin:0;font-size:11px;color:%s;
                           text-align:center;line-height:1.8;">
                  <strong style="color:%s;">GrillBros</strong> ·
                  support@grillbros.com · +233 XX XXX XXXX<br/>
                  This is an automatically generated receipt.
                  Please retain for your records.<br/>
                  <span style="font-size:10px;">
                    © %d GrillBros. All rights reserved.
                  </span>
                </p>
              </td>
            </tr>
            """.formatted(
                LIGHT_BG, BORDER, MUTED, DARK,
                java.time.Year.now().getValue()));

        // ── Orange bottom bar ──────────────────────────────────────────────
        sb.append("""
            <tr>
              <td style="background-color:%s;padding:0;height:6px;"></td>
            </tr>
            """.formatted(ORANGE));

        // Close card, wrapper, body, html
        sb.append("""
              </table>
            </td></tr></table>
            </body>
            </html>
            """);

        return sb.toString();
    }

    private static void addMetaRow(StringBuilder sb, String label,
                                   String value, boolean shaded) {
        String bg = shaded ? WHITE : LIGHT_BG;
        sb.append("""
            <tr style="background-color:%s;">
              <td style="padding:10px 16px;font-size:12px;color:%s;
                          font-weight:600;width:35%%;
                          border-bottom:1px solid %s;">%s</td>
              <td style="padding:10px 16px;font-size:13px;color:%s;
                          border-bottom:1px solid %s;
                          font-family:monospace;">%s</td>
            </tr>
            """.formatted(bg, MUTED, BORDER, label, DARK, BORDER, esc(value)));
    }

    private static String fmt(BigDecimal v) {
        return v != null ? String.format("%,.2f", v) : "0.00";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
