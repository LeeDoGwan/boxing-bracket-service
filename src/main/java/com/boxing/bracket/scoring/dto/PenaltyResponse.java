package com.boxing.bracket.scoring.dto;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.scoring.domain.Penalty;

public class PenaltyResponse {

    private final Long penaltyId;
    private final Long boutId;
    private final BoutSide targetSide;
    private final Integer penaltyPoint;
    private final String reason;
    private final Long createdBy;

    private PenaltyResponse(
            Long penaltyId,
            Long boutId,
            BoutSide targetSide,
            Integer penaltyPoint,
            String reason,
            Long createdBy
    ) {
        this.penaltyId = penaltyId;
        this.boutId = boutId;
        this.targetSide = targetSide;
        this.penaltyPoint = penaltyPoint;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    public static PenaltyResponse from(Penalty penalty) {
        return new PenaltyResponse(
                penalty.getId(),
                penalty.getBoutId(),
                penalty.getTargetSide(),
                penalty.getPenaltyPoint(),
                penalty.getReason(),
                penalty.getCreatedBy()
        );
    }

    public Long getPenaltyId() {
        return penaltyId;
    }

    public Long getBoutId() {
        return boutId;
    }

    public BoutSide getTargetSide() {
        return targetSide;
    }

    public Integer getPenaltyPoint() {
        return penaltyPoint;
    }

    public String getReason() {
        return reason;
    }

    public Long getCreatedBy() {
        return createdBy;
    }
}
