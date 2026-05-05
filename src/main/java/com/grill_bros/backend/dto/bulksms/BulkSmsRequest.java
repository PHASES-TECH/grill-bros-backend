package com.grill_bros.backend.dto.bulksms;

import com.grill_bros.backend.records.RecipientType;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class BulkSmsRequest {
    private RecipientType recipientType;
    private String message;
}
