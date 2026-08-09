package com.boxing.bracket.bout.dto;

import com.boxing.bracket.scoring.domain.RoundScore;

public class BoutRoundScoreResponse {

    private final Integer roundNo;
    private final Integer judgeNo;
    private final Integer redScore;
    private final Integer blueScore;

    private BoutRoundScoreResponse(Integer roundNo, Integer judgeNo, Integer redScore, Integer blueScore) {
        this.roundNo = roundNo;
        this.judgeNo = judgeNo;
        this.redScore = redScore;
        this.blueScore = blueScore;
    }

    public static BoutRoundScoreResponse of(RoundScore roundScore, Integer judgeNo) {
        return new BoutRoundScoreResponse(
                roundScore.getRoundNo(),
                judgeNo,
                roundScore.getRedScore(),
                roundScore.getBlueScore()
        );
    }

    public Integer getRoundNo() {
        return roundNo;
    }

    public Integer getJudgeNo() {
        return judgeNo;
    }

    public Integer getRedScore() {
        return redScore;
    }

    public Integer getBlueScore() {
        return blueScore;
    }
}
