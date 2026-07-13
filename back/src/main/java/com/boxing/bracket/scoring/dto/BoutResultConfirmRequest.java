package com.boxing.bracket.scoring.dto;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.scoring.domain.DecisionType;

import javax.validation.constraints.NotNull;

public class BoutResultConfirmRequest {

    @NotNull(message = "winnerSide is required")
    private BoutSide winnerSide;

    @NotNull(message = "decisionType is required")
    private DecisionType decisionType;

    @NotNull(message = "confirmedBy is required")
    private Long confirmedBy;

    protected BoutResultConfirmRequest() {
    }

    public BoutResultConfirmRequest(BoutSide winnerSide, DecisionType decisionType, Long confirmedBy) {
        this.winnerSide = winnerSide;
        this.decisionType = decisionType;
        this.confirmedBy = confirmedBy;
    }

    public BoutSide getWinnerSide() {
        return winnerSide;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public Long getConfirmedBy() {
        return confirmedBy;
    }
}
