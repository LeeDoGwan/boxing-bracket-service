package com.boxing.bracket.ringmanager.dto;

import com.boxing.bracket.bout.domain.BoutStatus;

import javax.validation.constraints.NotNull;

public class BoutStatusUpdateRequest {

    @NotNull(message = "status is required")
    private BoutStatus status;

    protected BoutStatusUpdateRequest() {
    }

    public BoutStatusUpdateRequest(BoutStatus status) {
        this.status = status;
    }

    public BoutStatus getStatus() {
        return status;
    }
}
