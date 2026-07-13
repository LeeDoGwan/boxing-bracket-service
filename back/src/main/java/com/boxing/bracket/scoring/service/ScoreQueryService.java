package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class ScoreQueryService {

    private final BoutRepository boutRepository;
    private final RoundScoreRepository roundScoreRepository;

    public ScoreQueryService(BoutRepository boutRepository, RoundScoreRepository roundScoreRepository) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
    }

    public List<RoundScoreResponse> getBoutScores(Long boutId) {
        return getBoutScores(boutId, null);
    }

    public List<RoundScoreResponse> getBoutScores(Long boutId, Long judgeId) {
        validateBoutId(boutId);

        if (!boutRepository.existsById(boutId)) {
            throw new BoutNotFoundException();
        }

        List<RoundScore> scores = judgeId == null
                ? roundScoreRepository.findByBoutIdOrderByRoundNoAscJudgeIdAsc(boutId)
                : roundScoreRepository.findByBoutIdAndJudgeId(boutId, judgeId);

        return scores.stream()
                .map(RoundScoreResponse::from)
                .collect(Collectors.toList());
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }
}
