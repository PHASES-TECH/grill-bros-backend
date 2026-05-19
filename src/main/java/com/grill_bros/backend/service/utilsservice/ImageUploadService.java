package com.grill_bros.backend.service.utilsservice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageUploadService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket.menu-items}")
    private String bucket;

    private final RestClient restClient;

    public String upload(MultipartFile file) {
        try {
            String fileName = UUID.randomUUID() + "_" +
                    sanitize(file.getOriginalFilename());

            String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

            restClient.post()
                    .uri(uploadUrl)
                    .header("Authorization", "Bearer " + supabaseKey)
                    .header("Content-Type", file.getContentType())
                    .body(file.getBytes())
                    .retrieve()
                    .toBodilessEntity();

            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + fileName;
            log.info("Image uploaded to Supabase: {}", publicUrl);
            return publicUrl;

        } catch (IOException e) {
            log.error("Image upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    public void delete(String imageUrl) {
        try {
            String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + fileName;

            restClient.delete()
                    .uri(deleteUrl)
                    .header("Authorization", "Bearer " + supabaseKey);

            log.info("Image deleted from Supabase: {}", fileName);

        } catch (Exception e) {
            log.error("Image deletion failed for {}: {}", imageUrl, e.getMessage(), e);
            throw new RuntimeException("Image deletion failed", e);
        }
    }

    private String sanitize(String filename) {
        if (filename == null) return "upload";
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}