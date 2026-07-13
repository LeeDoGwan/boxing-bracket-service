package com.boxing.bracket.scoring.dto;

import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;

import java.time.LocalDateTime;

public class RoundScoreResponse {

    private final Long scoreId;
    private final Long boutId;
    private final Integer roundNo;
    private final Long judgeId;
    private final Integer redScore;
    private final Integer blueScore;
    private final RoundScoreStatus status;
    private final LocalDateTime submittedAt;

    private RoundScoreResponse(
            Long scoreId,
            Long boutId,
            Integer roundNo,
            Long judgeId,
            Integer redScore,
            Integer blueScore,
            RoundScoreStatus status,
            LocalDateTime submittedAt
    ) {
        this.scoreId = scoreId;
        this.boutId = boutId;
        this.roundNo = roundNo;
        this.judgeId = judgeId;
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public static RoundScoreResponse from(RoundScore roundScore) {
        return new RoundScoreResponse(
                roundScore.getId(),
                roundScore.getBoutId(),
                roundScore.getRoundNo(),
                roundScore.getJudgeId(),
                roundScore.getRedScore(),
                roundScore.getBlueScore(),
                roundScore.getStatus(),
                roundScore.getSubmittedAt()
        );
    }

    public Long getScoreId() {
        return scoreId;
    }

    public Long getBoutId() {
        return boutId;
    }

    public Integer getRoundNo() {
        return roundNo;
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

    public RoundScoreStatus getStatus() {
        return status;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
