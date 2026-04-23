package com.grill_bros.backend.dto.bulksms;

import lombok.Data;

import java.util.UUID;

@Data
public class SmsCampaignResponse {
    private UUID id;
    private String status;
    private int totalRecipients;
    private int sentCount;
    private int failedCount;
    private String message;
}
