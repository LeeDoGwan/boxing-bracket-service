package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.dto.BoutResultResponse;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Lazy
@Transactional
public class SupervisorResultService {

    private final BoutRepository boutRepository;
    private final RoundScoreRepository roundScoreRepository;
    private final PenaltyRepository penaltyRepository;
    private final BoutResultRepository boutResultRepository;

    public SupervisorResultService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            PenaltyRepository penaltyRepository,
            BoutResultRepository boutResultRepository
    ) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
        this.penaltyRepository = penaltyRepository;
        this.boutResultRepository = boutResultRepository;
    }

    public BoutResultResponse confirmResult(Long boutId, BoutResultConfirmRequest request) {
        validateBoutId(boutId);
        validateRequest(request);

        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        List<RoundScore> roundScores = roundScoreRepository.findByBoutId(boutId);
        List<Penalty> penalties = penaltyRepository.findByBoutId(boutId);

        int redTotalScore = sumRedScores(roundScores);
        int blueTotalScore = sumBlueScores(roundScores);
        int redPenaltyTotal = sumPenalties(penalties, BoutSide.RED);
        int bluePenaltyTotal = sumPenalties(penalties, BoutSide.BLUE);

        BoutResult boutResult = boutResultRepository.findByBoutId(boutId)
                .orElseGet(() -> BoutResult.builder().boutId(boutId).build());
        boutResult.confirm(
                redTotalScore,
                blueTotalScore,
                redPenaltyTotal,
                bluePenaltyTotal,
                request.getWinnerSide(),
                request.getDecisionType(),
                request.getConfirmedBy()
        );

        bout.finish(request.getWinnerSide());
        bout.confirmResult(request.getWinnerSide());
        boutRepository.save(bout);

        return BoutResultResponse.from(boutResultRepository.save(boutResult));
    }

    private int sumRedScores(List<RoundScore> roundScores) {
        return roundScores.stream()
                .filter(roundScore -> roundScore.getStatus() == RoundScoreStatus.SUBMITTED)
                .map(RoundScore::getRedScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int sumBlueScores(List<RoundScore> roundScores) {
        return roundScores.stream()
                .filter(roundScore -> roundScore.getStatus() == RoundScoreStatus.SUBMITTED)
                .map(RoundScore::getBlueScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private int sumPenalties(List<Penalty> penalties, BoutSide targetSide) {
        return penalties.stream()
                .filter(penalty -> penalty.getTargetSide() == targetSide)
                .map(Penalty::getPenaltyPoint)
                .filter(point -> point != null)
                .mapToInt(Integer::intValue)
                .sum();
    }

    private void validateBoutId(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }
    }

    private void validateRequest(BoutResultConfirmRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("result request is required");
        }
        if (request.getWinnerSide() == null) {
            throw new IllegalArgumentException("winnerSide is required");
        }
        if (request.getDecisionType() == null) {
            throw new IllegalArgumentException("decisionType is required");
        }
        if (request.getConfirmedBy() == null) {
            throw new IllegalArgumentException("confirmedBy is required");
        }
    }
}
