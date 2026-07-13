package com.boxing.bracket.bout.admin.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;

public class AdminBoutResponse {

    private final Long boutId;
    private final Long tournamentId;
    private final Long ringId;
    private final Integer boutNumber;
    private final String matchType;
    private final Long redAthleteId;
    private final Long blueAthleteId;
    private final BoutStatus status;
    private final Integer totalRounds;
    private final Integer scheduledOrder;
    private final boolean eventBout;

    private AdminBoutResponse(
            Long boutId,
            Long tournamentId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            Long redAthleteId,
            Long blueAthleteId,
            BoutStatus status,
            Integer totalRounds,
            Integer scheduledOrder,
            boolean eventBout
    ) {
        this.boutId = boutId;
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.redAthleteId = redAthleteId;
        this.blueAthleteId = blueAthleteId;
        this.status = status;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.eventBout = eventBout;
    }

    public static AdminBoutResponse from(Bout bout) {
        return new AdminBoutResponse(
                bout.getId(),
                bout.getTournamentId(),
                bout.getRingId(),
                bout.getBoutNumber(),
                bout.getMatchType(),
                bout.getRedAthleteId(),
                bout.getBlueAthleteId(),
                bout.getStatus(),
                bout.getTotalRounds(),
                bout.getScheduledOrder(),
                bout.isEventBout()
        );
    }

    public Long getBoutId() {
        return boutId;
    }

    public Long getTournamentId() {
        return tournamentId;
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

    public Long getRedAthleteId() {
        return redAthleteId;
    }

    public Long getBlueAthleteId() {
        return blueAthleteId;
    }

    public BoutStatus getStatus() {
        return status;
    }

    public Integer getTotalRounds() {
        return totalRounds;
    }

    public Integer getScheduledOrder() {
        return scheduledOrder;
    }

    public boolean isEventBout() {
        return eventBout;
    }
}
