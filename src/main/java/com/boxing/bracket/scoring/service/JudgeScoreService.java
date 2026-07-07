package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
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

    public JudgeScoreService(BoutRepository boutRepository, RoundScoreRepository roundScoreRepository) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
    }

    public RoundScoreResponse submitRoundScore(Long boutId, Integer roundNo, RoundScoreSubmitRequest request) {
        validatePathVariables(boutId, roundNo);
        validateRequest(request);

        if (!boutRepository.existsById(boutId)) {
            throw new BoutNotFoundException();
        }

        RoundScore roundScore = roundScoreRepository
                .findByBoutIdAndRoundNoAndJudgeId(boutId, roundNo, request.getJudgeId())
                .orElseGet(() -> RoundScore.builder()
                        .boutId(boutId)
                        .roundNo(roundNo)
                        .judgeId(request.getJudgeId())
                        .build());

        roundScore.submit(request.getRedScore(), request.getBlueScore());

        return RoundScoreResponse.from(roundScoreRepository.save(roundScore));
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
