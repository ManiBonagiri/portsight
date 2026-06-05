package com.portsight.api.shared.exception;

import java.time.LocalDateTime;

public class ErrorResponse {
    private boolean success;
    private ErrorDetails error;

    public ErrorResponse() {}

    public ErrorResponse(boolean success, ErrorDetails error) {
        this.success = success;
        this.error = error;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public ErrorDetails getError() { return error; }
    public void setError(ErrorDetails error) { this.error = error; }

    public static class ErrorDetails {
        private String code;
        private String message;
        private LocalDateTime timestamp;

        public ErrorDetails() {}

        public ErrorDetails(String code, String message, LocalDateTime timestamp) {
            this.code = code;
            this.message = message;
            this.timestamp = timestamp;
        }

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    }
}
