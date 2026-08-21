package com.mock.exception;

import java.time.Instant;
import java.util.List;

public class ErrorResponse {
    private final String error;
    private final String message;
    private final int status;
    private final String timestamp;
    private final List<String> details;

    public ErrorResponse(String error, String message, int status, List<String> details) {
        this.error = error;
        this.message = message;
        this.status = status;
        this.timestamp = Instant.now().toString();
        this.details = details;
    }

    public ErrorResponse(String error, String message, int status) {
        this(error, message, status, null);
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public List<String> getDetails() {
        return details;
    }
}
