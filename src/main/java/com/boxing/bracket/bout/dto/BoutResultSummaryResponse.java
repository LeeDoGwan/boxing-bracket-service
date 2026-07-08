package com.boxing.bracket.bout.dto;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;

import java.time.LocalDateTime;

public class BoutResultSummaryResponse {

    private final Long resultId;
    private final Integer redTotalScore;
    private final Integer blueTotalScore;
    private final Integer redPenaltyTotal;
    private final Integer bluePenaltyTotal;
    private final BoutSide winnerSide;
    private final DecisionType decisionType;
    private final LocalDateTime confirmedAt;

    private BoutResultSummaryResponse(
            Long resultId,
            Integer redTotalScore,
            Integer blueTotalScore,
            Integer redPenaltyTotal,
            Integer bluePenaltyTotal,
            BoutSide winnerSide,
            DecisionType decisionType,
            LocalDateTime confirmedAt
    ) {
        this.resultId = resultId;
        this.redTotalScore = redTotalScore;
        this.blueTotalScore = blueTotalScore;
        this.redPenaltyTotal = redPenaltyTotal;
        this.bluePenaltyTotal = bluePenaltyTotal;
        this.winnerSide = winnerSide == null ? BoutSide.NONE : winnerSide;
        this.decisionType = decisionType == null ? DecisionType.UNKNOWN : decisionType;
        this.confirmedAt = confirmedAt;
    }

    public static BoutResultSummaryResponse from(BoutResult boutResult) {
        if (boutResult == null) {
            return null;
        }
        return new BoutResultSummaryResponse(
                boutResult.getId(),
                boutResult.getRedTotalScore(),
                boutResult.getBlueTotalScore(),
                boutResult.getRedPenaltyTotal(),
                boutResult.getBluePenaltyTotal(),
                boutResult.getWinnerSide(),
                boutResult.getDecisionType(),
                boutResult.getConfirmedAt()
        );
    }

    public Long getResultId() {
        return resultId;
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

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
