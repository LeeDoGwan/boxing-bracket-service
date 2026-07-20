package com.boxing.bracket.assignment.dto;

import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;

public class AssignedRingResponse {

    private final Long ringId;
    private final Long tournamentId;
    private final String name;
    private final RingStatus status;
    private final Long currentBoutId;

    private AssignedRingResponse(Ring ring) {
        this.ringId = ring.getId();
        this.tournamentId = ring.getTournamentId();
        this.name = ring.getName();
        this.status = ring.getStatus();
        this.currentBoutId = ring.getCurrentBoutId();
    }

    public static AssignedRingResponse from(Ring ring) {
        return new AssignedRingResponse(ring);
    }

    public Long getRingId() { return ringId; }
    public Long getTournamentId() { return tournamentId; }
    public String getName() { return name; }
    public RingStatus getStatus() { return status; }
    public Long getCurrentBoutId() { return currentBoutId; }
}
