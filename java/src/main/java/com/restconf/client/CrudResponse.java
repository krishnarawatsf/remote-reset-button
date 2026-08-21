package com.restconf.client;

public class CrudResponse {
    private final int statusCode;
    private final String body;
    private final boolean success;

    public CrudResponse(int statusCode, String body, boolean success) {
        this.statusCode = statusCode;
        this.body = body;
        this.success = success;
    }

    public int getStatusCode() { return statusCode; }
    public String getBody() { return body; }
    public boolean isSuccess() { return success; }
}
