package com.grill_bros.backend.dto.bulksms;

import lombok.Data;

import java.util.UUID;

@Data
public class RecipientPreview {
    private UUID id;
    private String name;
    private String phoneNumber;
    private String location;
}
