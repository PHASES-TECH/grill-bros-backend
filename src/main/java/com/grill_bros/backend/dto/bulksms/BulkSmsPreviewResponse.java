package com.grill_bros.backend.dto.bulksms;

import lombok.Data;

import java.util.List;

@Data
public class BulkSmsPreviewResponse {
    private int totalRecipients;
    private List<RecipientPreview> recipients;
    private String estimatedCost;
}
