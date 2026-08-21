package com.restconf.client;

/**
 * Standard response wrapper for RESTCONF operations.
 */
public class CrudResponse {

    private final int statusCode;
    private final String body;
    private final boolean success;

    public CrudResponse(int statusCode, String body, boolean success) {
        this.statusCode = statusCode;
        this.body = body != null ? body : "";
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }

    public boolean isConflict() {
        return statusCode == 409;
    }

    public boolean isUnauthorized() {
        return statusCode == 401;
    }

    @Override
    public String toString() {
        return "CrudResponse{" +
                "statusCode=" + statusCode +
                ", success=" + success +
                ", body='" + body + '\'' +
                '}';
    }
}
