package com.boxing.bracket.scoring.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

public class RoundScoreSubmitRequest {

    @NotNull(message = "judgeId is required")
    private Long judgeId;

    @NotNull(message = "redScore is required")
    @Min(value = 0, message = "redScore must be greater than or equal to 0")
    private Integer redScore;

    @NotNull(message = "blueScore is required")
    @Min(value = 0, message = "blueScore must be greater than or equal to 0")
    private Integer blueScore;

    protected RoundScoreSubmitRequest() {
    }

    public RoundScoreSubmitRequest(Long judgeId, Integer redScore, Integer blueScore) {
        this.judgeId = judgeId;
        this.redScore = redScore;
        this.blueScore = blueScore;
    }

    public Long getJudgeId() {
        return judgeId;
    }

    public Integer getRedScore() {
        return redScore;
    }

    public Integer getBlueScore() {
        return blueScore;
    }
}
