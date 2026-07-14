package com.boxing.bracket.operation.dto;

import com.boxing.bracket.bout.domain.BoutStatus;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class TournamentOperationStatusResponse {

    private final Long tournamentId;
    private final int totalBoutCount;
    private final Map<BoutStatus, Integer> boutStatusCounts;
    private final List<OperationRingStatusResponse> rings;
    private final List<JudgeScoreSubmissionStatusResponse> judgeScoreSubmissionStatuses;
    private final List<OperationBoutResponse> pendingResultBouts;
    private final List<OperationBoutResponse> stalledBouts;

    private TournamentOperationStatusResponse(
            Long tournamentId,
            int totalBoutCount,
            Map<BoutStatus, Integer> boutStatusCounts,
            List<OperationRingStatusResponse> rings,
            List<JudgeScoreSubmissionStatusResponse> judgeScoreSubmissionStatuses,
            List<OperationBoutResponse> pendingResultBouts,
            List<OperationBoutResponse> stalledBouts
    ) {
        this.tournamentId = tournamentId;
        this.totalBoutCount = totalBoutCount;
        this.boutStatusCounts = boutStatusCounts;
        this.rings = rings;
        this.judgeScoreSubmissionStatuses = judgeScoreSubmissionStatuses;
        this.pendingResultBouts = pendingResultBouts;
        this.stalledBouts = stalledBouts;
    }

    public static TournamentOperationStatusResponse of(
            Long tournamentId,
            int totalBoutCount,
            Map<BoutStatus, Integer> boutStatusCounts,
            List<OperationRingStatusResponse> rings,
            List<JudgeScoreSubmissionStatusResponse> judgeScoreSubmissionStatuses,
            List<OperationBoutResponse> pendingResultBouts,
            List<OperationBoutResponse> stalledBouts
    ) {
        return new TournamentOperationStatusResponse(
                tournamentId,
                totalBoutCount,
                boutStatusCounts,
                rings,
                judgeScoreSubmissionStatuses,
                pendingResultBouts,
                stalledBouts
        );
    }
}
