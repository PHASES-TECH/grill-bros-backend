package com.grill_bros.backend.service.utilsservice;

import com.grill_bros.backend.exceptions.ResourceNotFoundException;
import com.grill_bros.backend.model.Order;
import com.grill_bros.backend.model.OrderItem;
import com.grill_bros.backend.model.OrderItemModifier;
import com.grill_bros.backend.model.Receipt;
import com.grill_bros.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    //    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender mailSender;
    private final OrderRepository orderRepository;

    private static final String FROM_ADDRESS = "noreply@grillbros.com";
    private static final String FROM_NAME = "GrillBros";

    @Value("${mail.from}")
    private String senderEmail;

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                    .withZone(ZoneId.of("Africa/Accra"));

    @Async
    public void sendOtpEmail(String to, String name, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8"
            );

            helper.setTo(to);
            helper.setSubject(buildOtpSubject());

            String htmlBody = buildOtpHtml(name, otp);
            String plainBody = buildOtpPlainText(name, otp);

            helper.setText(plainBody, htmlBody);
            helper.setFrom(senderEmail, "GrillBros");

            mailSender.send(message);
            log.info("OTP email sent successfully to {}", to);

        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send OTP email", e); // preserve cause
        }
    }

    private String buildOtpSubject() {
        return "🔐 Your GrillBros Verification Code";
    }

    @Async
    public void sendReceiptEmail(String to, Receipt receipt, byte[] pdfBytes) {
        // Fetch order with all items + modifiers eagerly BEFORE any lazy access
        Order order = orderRepository.findByIdWithItems(receipt.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    "UTF-8");

            helper.setTo(to);
            helper.setSubject("🧾 Your GrillBros Receipt — " + order.getOrderNumber()); // use already-loaded order
            helper.setFrom(senderEmail, "GrillBros");

            String htmlBody = ReceiptEmailTemplate.build(receipt, order);
            String plainBody = buildPlainText(receipt, order); // pass order in

            helper.setText(plainBody, htmlBody);

            String attachmentName = "GrillBros-Receipt-" + order.getOrderNumber() + ".pdf";
            helper.addAttachment(attachmentName, new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Receipt email sent successfully to {} for order {}", to, order.getOrderNumber());

        } catch (Exception e) {
            log.error("Failed to send receipt email {}: {}", receipt.getReference(), e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    private String buildSubject(Receipt receipt) {
        Order order = orderRepository.findById(receipt.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        return "🧾 Your GrillBros Receipt — " + order.getOrderNumber();
    }

    /**
     * Plain text fallback for email clients that don't render HTML.
     * Mirrors the key information from the HTML template in a readable format.
     */

    private String buildOtpHtml(String name, String otp) {
        return """
                <div style="font-family: Arial, sans-serif; background-color:#f9fafb; padding:20px;">
                  <div style="max-width:500px; margin:auto; background:#ffffff; border-radius:12px; padding:24px; box-shadow:0 4px 10px rgba(0,0,0,0.05);">
                
                    <h2 style="color:#111827; margin-bottom:16px;">GrillBros Verification</h2>
                
                    <p style="color:#374151; font-size:14px;">
                      Hi %s,
                    </p>
                
                    <p style="color:#374151; font-size:14px;">
                      Use the verification code below to continue:
                    </p>
                
                    <div style="margin:20px 0; text-align:center;">
                      <span style="display:inline-block; font-size:24px; letter-spacing:4px; font-weight:bold; background:#f3f4f6; padding:12px 20px; border-radius:8px;">
                        %s
                      </span>
                    </div>
                
                    <p style="color:#6b7280; font-size:13px;">
                      This code will expire in 10 minutes. Do not share it with anyone.
                    </p>
                
                    <hr style="margin:20px 0; border:none; border-top:1px solid #e5e7eb;" />
                
                    <p style="font-size:12px; color:#9ca3af;">
                      If you did not request this, please ignore this email.
                    </p>
                
                    <p style="font-size:12px; color:#9ca3af;">
                      GrillBros<br/>
                      support@grillbros.com
                    </p>
                
                  </div>
                </div>
                """.formatted(name, otp);
    }

    private String buildOtpPlainText(String name, String otp) {
        StringBuilder sb = new StringBuilder();

        sb.append("GRILLBROS — VERIFICATION CODE\n");
        sb.append("================================\n\n");

        sb.append("Hi ").append(name).append(",\n\n");

        sb.append("Use the code below to verify your account:\n\n");

        sb.append("CODE: ").append(otp).append("\n\n");

        sb.append("This code will expire in 10 minutes.\n");
        sb.append("Do not share this code with anyone.\n\n");

        sb.append("-----------------------------\n");
        sb.append("If you did not request this, please ignore this email.\n\n");

        sb.append("GrillBros\n");
        sb.append("support@grillbros.com\n");
        sb.append("This is an automatically generated email. Please do not reply.\n");

        return sb.toString();
    }

    private String buildPlainText(Receipt receipt, Order order) {
        StringBuilder sb = new StringBuilder();

        sb.append("THE GRILLBROS — PAYMENT RECEIPT\n");
        sb.append("============================\n\n");

        sb.append("Hi ").append(receipt.getCustomerName()).append(",\n\n");
        sb.append("Your payment was received successfully. ")
                .append("A styled PDF receipt is attached to this email.\n\n");

        sb.append("RECEIPT DETAILS\n");
        sb.append("---------------\n");
        sb.append("Reference:  ").append(receipt.getReference()).append("\n");
        sb.append("Order No.:  ").append(order.getOrderNumber()).append("\n");
        sb.append("Phone:      ").append(receipt.getCustomerPhone()).append("\n");
        sb.append("Date:       ").append(DATE_FMT.format(receipt.getIssuedAt())).append("\n\n");

        sb.append("ORDER ITEMS\n");
        sb.append("-----------\n");
        log.info("Order Items: {}", order.getItems());
        for (OrderItem item : order.getItems()) {
            sb.append(String.format("%-30s  x%-3d  GHS %s%n",
                    item.getItemName(),
                    item.getQuantity(),
                    String.format("%,.2f", item.getLineTotal())));

            Set<OrderItemModifier> modifiers = item.getModifiers();
            log.info("MODIFIERS: {}", modifiers);

            if (modifiers != null && !modifiers.isEmpty()) {
                for (OrderItemModifier itemModifier : modifiers) {

                    sb.append(String.format("%-30s    GHS %s%n",
                            itemModifier.getModifier().getName(),
                            itemModifier.getModifier().getPrice()
                    ));
                }
            }
        }

        sb.append("\n");
        sb.append(String.format("%-36s  GHS %s%n",
                "TOTAL AMOUNT",
                String.format("%,.2f", receipt.getAmount())));

        sb.append("\n-----------------------------\n");
        sb.append("Thank you for dining with GrillBros!\n");
        sb.append("We hope you enjoy every bite. See you soon!\n\n");

        sb.append("GrillBros\n");
        sb.append("support@grillbros.com\n");
        sb.append("This is an automatically generated email. Please do not reply.\n");

        return sb.toString();
    }
}
