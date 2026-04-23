package com.grill_bros.backend.dto.bulksms;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SmsResponse {

    private boolean success;
    private String messageId;
    private String errorMessage;

}
