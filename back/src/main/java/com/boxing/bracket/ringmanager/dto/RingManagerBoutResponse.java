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
    private final Integer totalRounds;
    private final Integer scheduledOrder;
    private final boolean resultConfirmed;
    private final LocalDateTime startedAt;

    private RingManagerBoutResponse(
            Long boutId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            BoutStatus status,
            Integer currentRound,
            Integer totalRounds,
            Integer scheduledOrder,
            boolean resultConfirmed,
            LocalDateTime startedAt
    ) {
        this.boutId = boutId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.status = status;
        this.currentRound = currentRound;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.resultConfirmed = resultConfirmed;
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
                bout.getTotalRounds(),
                bout.getScheduledOrder(),
                bout.isResultConfirmed(),
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

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public Integer getScheduledOrder() {
        return scheduledOrder;
    }

    public boolean isResultConfirmed() {
        return resultConfirmed;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }
}
