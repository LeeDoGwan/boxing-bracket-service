package com.boxing.bracket.operation.dto;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class JudgeScoreSubmissionStatusResponse {

    private final Long boutId;
    private final Integer boutNumber;
    private final Integer roundNo;
    private final List<Long> submittedJudgeIds;
    private final List<Long> unsubmittedJudgeIds;
    private final boolean complete;

    private JudgeScoreSubmissionStatusResponse(
            Long boutId,
            Integer boutNumber,
            Integer roundNo,
            List<Long> submittedJudgeIds,
            List<Long> unsubmittedJudgeIds
    ) {
        this.boutId = boutId;
        this.boutNumber = boutNumber;
        this.roundNo = roundNo;
        this.submittedJudgeIds = submittedJudgeIds;
        this.unsubmittedJudgeIds = unsubmittedJudgeIds;
        this.complete = !submittedJudgeIds.isEmpty() && unsubmittedJudgeIds.isEmpty();
    }

    public static JudgeScoreSubmissionStatusResponse of(
            Bout bout,
            Integer roundNo,
            List<RoundScore> roundScores
    ) {
        return new JudgeScoreSubmissionStatusResponse(
                bout.getId(),
                bout.getBoutNumber(),
                roundNo,
                judgeIdsByStatus(roundScores, RoundScoreStatus.SUBMITTED),
                judgeIdsByStatus(roundScores, RoundScoreStatus.DRAFT)
        );
    }

    private static List<Long> judgeIdsByStatus(List<RoundScore> roundScores, RoundScoreStatus status) {
        return roundScores.stream()
                .filter(roundScore -> roundScore.getStatus() == status)
                .map(RoundScore::getJudgeId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
    }
}
