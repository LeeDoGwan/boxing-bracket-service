package com.boxing.bracket.ringmanager.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;

import java.time.LocalDateTime;

public class RingManagerBoutResponse {

    private final Long boutId;
    private final Long ringId;
    private final Integer boutNumber;
    private final String matchType;
    private final BoutStatus status;
    private final Integer currentRound;
    private final Integer scheduledOrder;
    private final LocalDateTime startedAt;

    private RingManagerBoutResponse(
            Long boutId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            BoutStatus status,
            Integer currentRound,
            Integer scheduledOrder,
            LocalDateTime startedAt
    ) {
        this.boutId = boutId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.status = status;
        this.currentRound = currentRound;
        this.scheduledOrder = scheduledOrder;
        this.startedAt = startedAt;
    }

    public static RingManagerBoutResponse from(Bout bout) {
        return new RingManagerBoutResponse(
                bout.getId(),
                bout.getRingId(),
                bout.getBoutNumber(),
                bout.getMatchType(),
                bout.getStatus(),
                bout.getCurrentRound(),
                bout.getScheduledOrder(),
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

    public String getMatchType() {
        return matchType;
    }

    public BoutStatus getStatus() {
        return status;
    }

    public Integer getCurrentRound() {
        return currentRound;
    }

    public Integer getScheduledOrder() {
        return scheduledOrder;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}
