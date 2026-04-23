package com.grill_bros.backend.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class ErrorResponse{
    private int status;
    private String error;        // Error type/code for frontend
    private String message;      // Human-readable message
    private Instant timestamp;
    private String path;         // Optional: the request path

    public ErrorResponse(int status, String error, String message, String path) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = Instant.now();
        this.path = path;
    }
}

