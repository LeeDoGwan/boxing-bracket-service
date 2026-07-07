package com.boxing.bracket.health;

public class HealthResponse {

    private final String status;
    private final String service;

    public HealthResponse(String status, String service) {
        this.status = status;
        this.service = service;
    }

    public String getStatus() {
        return status;
    }

    public String getService() {
        return service;
    }
}
