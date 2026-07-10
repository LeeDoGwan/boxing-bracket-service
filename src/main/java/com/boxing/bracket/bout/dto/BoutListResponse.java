package com.boxing.bracket.bout.dto;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.scoring.domain.BoutResult;

public class BoutListResponse {

    private final Long boutId;
    private final Integer boutNumber;
    private final Long ringId;
    private final String matchType;
    private final AthleteSummaryResponse redAthlete;
    private final AthleteSummaryResponse blueAthlete;
    private final BoutStatus status;
    private final BoutSide winnerSide;
    private final boolean resultConfirmed;
    private final BoutResultSummaryResponse result;
    private final Integer scheduledOrder;

    private BoutListResponse(
            Long boutId,
            Integer boutNumber,
            Long ringId,
            String matchType,
            AthleteSummaryResponse redAthlete,
            AthleteSummaryResponse blueAthlete,
            BoutStatus status,
            BoutSide winnerSide,
            boolean resultConfirmed,
            BoutResultSummaryResponse result,
            Integer scheduledOrder
    ) {
        this.boutId = boutId;
        this.boutNumber = boutNumber;
        this.ringId = ringId;
        this.matchType = matchType;
        this.redAthlete = redAthlete;
        this.blueAthlete = blueAthlete;
        this.status = status;
        this.winnerSide = winnerSide == null ? BoutSide.NONE : winnerSide;
        this.resultConfirmed = resultConfirmed;
        this.result = result;
        this.scheduledOrder = scheduledOrder;
    }

    public static BoutListResponse of(Bout bout, Athlete redAthlete, Athlete blueAthlete) {
        return of(bout, redAthlete, blueAthlete, null);
    }

    public static BoutListResponse of(Bout bout, Athlete redAthlete, Athlete blueAthlete, BoutResult boutResult) {
        return new BoutListResponse(
                bout.getId(),
                bout.getBoutNumber(),
                bout.getRingId(),
                bout.getMatchType(),
                AthleteSummaryResponse.from(redAthlete),
                AthleteSummaryResponse.from(blueAthlete),
                bout.getStatus(),
                bout.getWinnerSide(),
                bout.isResultConfirmed(),
                BoutResultSummaryResponse.from(boutResult),
                bout.getScheduledOrder()
        );
    }

    public Long getBoutId() {
        return boutId;
    }

    public Integer getBoutNumber() {
        return boutNumber;
    }

    public Long getRingId() {
        return ringId;
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

    public BoutSide getWinnerSide() {
        return winnerSide;
    }

    public boolean isResultConfirmed() {
        return resultConfirmed;
    }

    public BoutResultSummaryResponse getResult() {
        return result;
    }

    public Integer getScheduledOrder() {
        return scheduledOrder;
    }
}
