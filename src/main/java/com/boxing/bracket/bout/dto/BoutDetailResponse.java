package com.boxing.bracket.bout.dto;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;

public class BoutDetailResponse {

    private final Long boutId;
    private final Long tournamentId;
    private final Long ringId;
    private final Integer boutNumber;
    private final String matchType;
    private final AthleteSummaryResponse redAthlete;
    private final AthleteSummaryResponse blueAthlete;
    private final BoutStatus status;
    private final Integer currentRound;
    private final Integer totalRounds;
    private final Integer scheduledOrder;
    private final BoutSide winnerSide;
    private final boolean resultConfirmed;
    private final boolean eventBout;

    private BoutDetailResponse(
            Long boutId,
            Long tournamentId,
            Long ringId,
            Integer boutNumber,
            String matchType,
            AthleteSummaryResponse redAthlete,
            AthleteSummaryResponse blueAthlete,
            BoutStatus status,
            Integer currentRound,
            Integer totalRounds,
            Integer scheduledOrder,
            BoutSide winnerSide,
            boolean resultConfirmed,
            boolean eventBout
    ) {
        this.boutId = boutId;
        this.tournamentId = tournamentId;
        this.ringId = ringId;
        this.boutNumber = boutNumber;
        this.matchType = matchType;
        this.redAthlete = redAthlete;
        this.blueAthlete = blueAthlete;
        this.status = status;
        this.currentRound = currentRound;
        this.totalRounds = totalRounds;
        this.scheduledOrder = scheduledOrder;
        this.winnerSide = winnerSide == null ? BoutSide.NONE : winnerSide;
        this.resultConfirmed = resultConfirmed;
        this.eventBout = eventBout;
    }

    public static BoutDetailResponse of(Bout bout, Athlete redAthlete, Athlete blueAthlete) {
        return new BoutDetailResponse(
                bout.getId(),
                bout.getTournamentId(),
                bout.getRingId(),
                bout.getBoutNumber(),
                bout.getMatchType(),
                AthleteSummaryResponse.from(redAthlete),
                AthleteSummaryResponse.from(blueAthlete),
                bout.getStatus(),
                bout.getCurrentRound(),
                bout.getTotalRounds(),
                bout.getScheduledOrder(),
                bout.getWinnerSide(),
                bout.isResultConfirmed(),
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

    public AthleteSummaryResponse getRedAthlete() {
        return redAthlete;
    }

    public AthleteSummaryResponse getBlueAthlete() {
        return blueAthlete;
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

    public BoutSide getWinnerSide() {
        return winnerSide;
    }

    public boolean isResultConfirmed() {
        return resultConfirmed;
    }

    public boolean isEventBout() {
        return eventBout;
    }
}
