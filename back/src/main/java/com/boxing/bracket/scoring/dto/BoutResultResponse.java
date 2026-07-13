package com.boxing.bracket.scoring.dto;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;

import java.time.LocalDateTime;

public class BoutResultResponse {

    private final Long resultId;
    private final Long boutId;
    private final Integer redTotalScore;
    private final Integer blueTotalScore;
    private final Integer redPenaltyTotal;
    private final Integer bluePenaltyTotal;
    private final BoutSide winnerSide;
    private final DecisionType decisionType;
    private final Long confirmedBy;
    private final LocalDateTime confirmedAt;

    private BoutResultResponse(
            Long resultId,
            Long boutId,
            Integer redTotalScore,
            Integer blueTotalScore,
            Integer redPenaltyTotal,
            Integer bluePenaltyTotal,
            BoutSide winnerSide,
            DecisionType decisionType,
            Long confirmedBy,
            LocalDateTime confirmedAt
    ) {
        this.resultId = resultId;
        this.boutId = boutId;
        this.redTotalScore = redTotalScore;
        this.blueTotalScore = blueTotalScore;
        this.redPenaltyTotal = redPenaltyTotal;
        this.bluePenaltyTotal = bluePenaltyTotal;
        this.winnerSide = winnerSide;
        this.decisionType = decisionType;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = confirmedAt;
    }

    public static BoutResultResponse from(BoutResult boutResult) {
        return new BoutResultResponse(
                boutResult.getId(),
                boutResult.getBoutId(),
                boutResult.getRedTotalScore(),
                boutResult.getBlueTotalScore(),
                boutResult.getRedPenaltyTotal(),
                boutResult.getBluePenaltyTotal(),
                boutResult.getWinnerSide(),
                boutResult.getDecisionType(),
                boutResult.getConfirmedBy(),
                boutResult.getConfirmedAt()
        );
    }

    public Long getResultId() {
        return resultId;
    }

    public Long getBoutId() {
        return boutId;
    }

    public Integer getRedTotalScore() {
        return redTotalScore;
    }

    public Integer getBlueTotalScore() {
        return blueTotalScore;
    }

    public Integer getRedPenaltyTotal() {
        return redPenaltyTotal;
    }

    public Integer getBluePenaltyTotal() {
        return bluePenaltyTotal;
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

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
