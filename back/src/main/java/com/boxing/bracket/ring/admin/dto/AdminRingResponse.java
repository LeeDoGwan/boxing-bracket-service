package com.boxing.bracket.ring.admin.dto;

import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;

public class AdminRingResponse {

    private final Long ringId;
    private final Long tournamentId;
    private final String name;
    private final RingStatus status;
    private final Long currentBoutId;

    private AdminRingResponse(Long ringId, Long tournamentId, String name, RingStatus status, Long currentBoutId) {
        this.ringId = ringId;
        this.tournamentId = tournamentId;
        this.name = name;
        this.status = status;
        this.currentBoutId = currentBoutId;
    }

    public static AdminRingResponse from(Ring ring) {
        return new AdminRingResponse(
                ring.getId(),
                ring.getTournamentId(),
                ring.getName(),
                ring.getStatus(),
                ring.getCurrentBoutId()
        );
    }

    public Long getRingId() {
        return ringId;
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

    public Long getCurrentBoutId() {
        return currentBoutId;
    }
}
