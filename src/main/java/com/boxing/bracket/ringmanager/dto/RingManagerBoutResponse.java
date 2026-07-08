package com.boxing.bracket.ringmanager.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;

import java.time.LocalDateTime;

public class RingManagerBoutResponse {

    private final Long boutId;
    private final Long ringId;
    private final Integer boutNumber;
    private final BoutStatus status;
    private final Integer currentRound;
    private final LocalDateTime startedAt;

    private RingManagerBoutResponse(
            Long boutId,
            Long ringId,
            Integer boutNumber,
            BoutStatus status,
            Integer currentRound,
            LocalDateTime startedAt
    ) {
        this.boutId = boutId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.status = status;
        this.currentRound = currentRound;
        this.startedAt = startedAt;
    }

    public static RingManagerBoutResponse from(Bout bout) {
        return new RingManagerBoutResponse(
                bout.getId(),
                bout.getRingId(),
                bout.getBoutNumber(),
                bout.getStatus(),
                bout.getCurrentRound(),
                bout.getStartedAt()
        );
    }

    public Long getBoutId() {
        return boutId;
    }

    public Long getRingId() {
        return ringId;
    }

    public Integer getBoutNumber() {
        return boutNumber;
    }

    public BoutStatus getStatus() {
        return status;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}
