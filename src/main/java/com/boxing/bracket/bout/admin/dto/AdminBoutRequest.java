package com.boxing.bracket.bout.admin.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

public class AdminBoutRequest {

    @NotNull(message = "tournamentId is required")
    private Long tournamentId;

    @NotNull(message = "ringId is required")
    private Long ringId;

    @NotNull(message = "boutNumber is required")
    @Positive(message = "boutNumber must be positive")
    private Integer boutNumber;

    private String matchType;

    @NotNull(message = "redAthleteId is required")
    private Long redAthleteId;

    @NotNull(message = "blueAthleteId is required")
    private Long blueAthleteId;

    @Positive(message = "totalRounds must be positive")
    private Integer totalRounds;

    @Positive(message = "scheduledOrder must be positive")
    private Integer scheduledOrder;

    private boolean eventBout;

    protected AdminBoutRequest() {
    }

    public AdminBoutRequest(
            Long tournamentId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            Long redAthleteId,
            Long blueAthleteId,
            Integer totalRounds,
            Integer scheduledOrder,
            boolean eventBout
    ) {
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.redAthleteId = redAthleteId;
        this.blueAthleteId = blueAthleteId;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.eventBout = eventBout;
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
