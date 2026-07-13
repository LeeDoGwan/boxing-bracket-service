package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.domain.BoutEventType;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
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
    private final BoutEventPublisher boutEventPublisher;

    public SupervisorResultService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            PenaltyRepository penaltyRepository,
            BoutResultRepository boutResultRepository,
            @Lazy BoutEventPublisher boutEventPublisher
    ) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
        this.penaltyRepository = penaltyRepository;
        this.boutResultRepository = boutResultRepository;
        this.boutEventPublisher = boutEventPublisher;
    }

    public BoutResultResponse confirmResult(Long boutId, BoutResultConfirmRequest request) {
        validateBoutId(boutId);
        validateRequest(request);

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        BoutResult existingResult = boutResultRepository.findByBoutId(boutId).orElse(null);
        if (bout.isResultConfirmed() || existingResult != null) {
            if (existingResult != null && existingResult.matchesConfirmation(
                    request.getWinnerSide(),
                    request.getDecisionType(),
                    request.getConfirmedBy()
            )) {
                return BoutResultResponse.from(existingResult);
            }
            throw new WorkflowConflictException("RESULT_ALREADY_CONFIRMED");
        }
        List<RoundScore> roundScores = roundScoreRepository.findByBoutId(boutId);
        List<Penalty> penalties = penaltyRepository.findByBoutId(boutId);

        int redTotalScore = sumRedScores(roundScores);
        int blueTotalScore = sumBlueScores(roundScores);
        int redPenaltyTotal = sumPenalties(penalties, BoutSide.RED);
        int bluePenaltyTotal = sumPenalties(penalties, BoutSide.BLUE);

        BoutResult boutResult = BoutResult.builder().boutId(boutId).build();
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
        Bout savedBout = boutRepository.save(bout);

        BoutResult savedResult = boutResultRepository.save(boutResult);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.RESULT_CONFIRMED, savedBout));
        return BoutResultResponse.from(savedResult);
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
