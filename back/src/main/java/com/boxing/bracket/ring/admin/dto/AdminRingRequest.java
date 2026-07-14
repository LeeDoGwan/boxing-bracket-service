package com.boxing.bracket.ring.admin.dto;

import com.boxing.bracket.ring.domain.RingStatus;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AdminRingRequest {

    @NotNull(message = "tournamentId is required")
    private Long tournamentId;

    @NotBlank(message = "name is required")
    private String name;

    private RingStatus status;

    protected AdminRingRequest() {
    }

    public AdminRingRequest(Long tournamentId, String name, RingStatus status) {
        this.tournamentId = tournamentId;
        this.name = name;
        this.status = status;
    }

    public Long getTournamentId() {
        return tournamentId;
    }

    public String getName() {
        return name;
    }

    public RingStatus getStatus() {
        return status;
    }
}
