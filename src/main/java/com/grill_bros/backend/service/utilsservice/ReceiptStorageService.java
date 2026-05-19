package com.grill_bros.backend.service.utilsservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket.receipts}")
    private String receiptsBucket;

    private final RestClient restClient;

    public String uploadReceiptPdf(
            byte[] pdfBytes,
            String receiptReference
    ) {

        try {

            String fileName =
                    receiptReference +
                            "-" +
                            UUID.randomUUID() +
                            ".pdf";

            String uploadUrl =
                    supabaseUrl +
                            "/storage/v1/object/" +
                            receiptsBucket +
                            "/" +
                            fileName;

            restClient.put()
                    .uri(uploadUrl)
                    .header(
                            "Authorization",
                            "Bearer " + supabaseKey
                    )
                    .header("apikey", supabaseKey)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes)
                    .retrieve()
                    .toBodilessEntity();

            String publicUrl =
                    supabaseUrl +
                            "/storage/v1/object/public/" +
                            receiptsBucket +
                            "/" +
                            fileName;

            log.info(
                    "Receipt uploaded successfully: {}",
                    publicUrl
            );

            return publicUrl;

        } catch (Exception e) {

            log.error(
                    "Receipt upload failed: {}",
                    e.getMessage(),
                    e
            );

            throw new RuntimeException(
                    "Failed to upload receipt PDF",
                    e
            );
        }
    }
}
