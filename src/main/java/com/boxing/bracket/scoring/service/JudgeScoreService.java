package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.domain.BoutEventType;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@Transactional
public class JudgeScoreService {

    private final BoutRepository boutRepository;
    private final RoundScoreRepository roundScoreRepository;
    private final BoutEventPublisher boutEventPublisher;

    public JudgeScoreService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            @Lazy BoutEventPublisher boutEventPublisher
    ) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
        this.boutEventPublisher = boutEventPublisher;
    }

    public RoundScoreResponse submitRoundScore(Long boutId, Integer roundNo, RoundScoreSubmitRequest request) {
        validatePathVariables(boutId, roundNo);
        validateRequest(request);

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        if (bout.isCompleted()) {
            throw new WorkflowConflictException("INVALID_BOUT_STATE");
        }

        RoundScore roundScore = roundScoreRepository
                .findByBoutIdAndRoundNoAndJudgeId(boutId, roundNo, request.getJudgeId())
                .orElseGet(() -> RoundScore.builder()
                        .boutId(boutId)
                        .roundNo(roundNo)
                        .judgeId(request.getJudgeId())
                        .build());

        boolean scoreSubmitted = roundScore.submit(request.getRedScore(), request.getBlueScore());
        if (!scoreSubmitted) {
            return RoundScoreResponse.from(roundScore);
        }

        RoundScore savedRoundScore = roundScoreRepository.save(roundScore);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.SCORE_SUBMITTED, bout, roundNo));
        return RoundScoreResponse.from(savedRoundScore);
    }

    private void validatePathVariables(Long boutId, Integer roundNo) {
        validateBoutId(boutId);
        if (roundNo == null) {
            throw new IllegalArgumentException("roundNo is required");
        }
        if (roundNo < 1) {
            throw new IllegalArgumentException("roundNo must be greater than or equal to 1");
        }
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }

    private void validateRequest(RoundScoreSubmitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("score request is required");
        }
        if (request.getJudgeId() == null) {
            throw new IllegalArgumentException("judgeId is required");
        }
        if (request.getRedScore() == null) {
            throw new IllegalArgumentException("redScore is required");
        }
        if (request.getBlueScore() == null) {
            throw new IllegalArgumentException("blueScore is required");
        }
    }
}
