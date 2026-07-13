package com.boxing.bracket.scoring.dto;

import com.boxing.bracket.bout.domain.BoutSide;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class PenaltyCreateRequest {

    @NotNull(message = "targetSide is required")
    private BoutSide targetSide;

    @NotNull(message = "penaltyPoint is required")
    @Min(value = 0, message = "penaltyPoint must be greater than or equal to 0")
    private Integer penaltyPoint;

    private String reason;

    @NotNull(message = "createdBy is required")
    private Long createdBy;

    protected PenaltyCreateRequest() {
    }

    public PenaltyCreateRequest(BoutSide targetSide, Integer penaltyPoint, String reason, Long createdBy) {
        this.targetSide = targetSide;
        this.penaltyPoint = penaltyPoint;
        this.reason = reason;
        this.createdBy = createdBy;
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
