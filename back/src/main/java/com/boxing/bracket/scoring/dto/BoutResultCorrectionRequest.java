package com.boxing.bracket.scoring.dto;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.scoring.domain.DecisionType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class BoutResultCorrectionRequest {

    @NotNull(message = "winnerSide is required")
    private BoutSide winnerSide;

    @NotNull(message = "decisionType is required")
    private DecisionType decisionType;

    @NotBlank(message = "reason is required")
    private String reason;

    private Long approvedBy;

    protected BoutResultCorrectionRequest() {
    }

    public BoutResultCorrectionRequest(
            BoutSide winnerSide,
            DecisionType decisionType,
            String reason,
            Long approvedBy
    ) {
        this.winnerSide = winnerSide;
        this.decisionType = decisionType;
        this.reason = reason;
        this.approvedBy = approvedBy;
    }

    public BoutSide getWinnerSide() {
        return winnerSide;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public String getReason() {
        return reason;
    }

    public Long getApprovedBy() {
        return approvedBy;
    }
}
