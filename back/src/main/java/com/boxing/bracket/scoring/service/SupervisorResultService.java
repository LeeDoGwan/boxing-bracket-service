package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.assignment.repository.StaffAssignmentRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.domain.BoutEventType;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.dto.BoutResultCorrectionRequest;
import com.boxing.bracket.scoring.dto.BoutResultResponse;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import com.boxing.bracket.user.domain.UserRole;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class SupervisorResultService {

    private final BoutRepository boutRepository;
    private final RoundScoreRepository roundScoreRepository;
    private final PenaltyRepository penaltyRepository;
    private final BoutResultRepository boutResultRepository;
    private final BoutEventPublisher boutEventPublisher;
    private final StaffAssignmentService staffAssignmentService;
    private final TournamentRepository tournamentRepository;
    private final StaffAssignmentRepository staffAssignmentRepository;

    public SupervisorResultService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            PenaltyRepository penaltyRepository,
            BoutResultRepository boutResultRepository,
            @Lazy BoutEventPublisher boutEventPublisher
    ) {
        this(boutRepository, roundScoreRepository, penaltyRepository, boutResultRepository, boutEventPublisher, null, null, null);
    }

    @Autowired
    public SupervisorResultService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            PenaltyRepository penaltyRepository,
            BoutResultRepository boutResultRepository,
            @Lazy BoutEventPublisher boutEventPublisher,
            @Lazy StaffAssignmentService staffAssignmentService,
            TournamentRepository tournamentRepository,
            StaffAssignmentRepository staffAssignmentRepository
    ) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
        this.penaltyRepository = penaltyRepository;
        this.boutResultRepository = boutResultRepository;
        this.boutEventPublisher = boutEventPublisher;
        this.staffAssignmentService = staffAssignmentService;
        this.tournamentRepository = tournamentRepository;
        this.staffAssignmentRepository = staffAssignmentRepository;
    }

    public BoutResultResponse confirmResult(Long boutId, BoutResultConfirmRequest request) {
        validateBoutId(boutId);
        validateRequest(request);
        Long actorId = SupervisorActorResolver.resolve(request.getConfirmedBy());
        if (staffAssignmentService != null) {
            staffAssignmentService.requireBoutAccess(boutId);
        }

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        BoutResult existingResult = boutResultRepository.findByBoutId(boutId).orElse(null);
        if (bout.isResultConfirmed() || existingResult != null) {
            if (existingResult != null && existingResult.matchesConfirmation(
                    request.getWinnerSide(),
                    request.getDecisionType(),
                    actorId
            )) {
                return BoutResultResponse.from(existingResult);
            }
            throw new WorkflowConflictException("RESULT_ALREADY_CONFIRMED");
        }
        bout.validateResultConfirmation();
        List<RoundScore> roundScores = roundScoreRepository.findByBoutId(boutId);
        validateScores(bout, roundScores);
        validateDecision(request);
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
                actorId
        );

        bout.finish(request.getWinnerSide());
        bout.confirmResult(request.getWinnerSide());
        Bout savedBout = boutRepository.save(bout);

        BoutResult savedResult = boutResultRepository.save(boutResult);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.RESULT_CONFIRMED, savedBout));
        return BoutResultResponse.from(savedResult);
    }

    public BoutResultResponse correctResult(Long boutId, BoutResultCorrectionRequest request) {
        validateBoutId(boutId);
        validateCorrectionRequest(request);
        Long actorId = SupervisorActorResolver.resolve(request.getApprovedBy());
        if (staffAssignmentService != null) {
            staffAssignmentService.requireBoutAccess(boutId);
        }

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        BoutResult boutResult = boutResultRepository.findByBoutId(boutId)
                .orElseThrow(() -> new WorkflowConflictException("RESULT_NOT_FOUND"));
        validateDecision(request.getWinnerSide(), request.getDecisionType());

        bout.correctResult(request.getWinnerSide());
        boutResult.correct(request.getWinnerSide(), request.getDecisionType(), actorId);
        Bout savedBout = boutRepository.save(bout);
        BoutResult savedResult = boutResultRepository.save(boutResult);
        boutEventPublisher.publish(BoutEventResponse.of(BoutEventType.RESULT_CORRECTED, savedBout));
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

    private void validateScores(Bout bout, List<RoundScore> roundScores) {
        if (roundScores == null || roundScores.isEmpty()
                || roundScores.stream().anyMatch(roundScore ->
                roundScore.getStatus() != RoundScoreStatus.SUBMITTED
                        || roundScore.getRedScore() == null
                        || roundScore.getBlueScore() == null)) {
            throw new WorkflowConflictException("SCORES_NOT_READY");
        }
        if (tournamentRepository == null || staffAssignmentRepository == null) {
            return;
        }
        Tournament tournament = tournamentRepository.findById(bout.getTournamentId()).orElse(null);
        if (tournament == null) {
            return;
        }
        List<StaffAssignment> judgeAssignments = staffAssignmentRepository
                .findByTournamentIdAndRingIdAndRoleAndActiveTrueOrderByAccountIdAsc(
                        bout.getTournamentId(), bout.getRingId(), UserRole.JUDGE
                );
        if (judgeAssignments.size() != tournament.getJudgeCount()) {
            throw new WorkflowConflictException("SCORES_NOT_READY");
        }
        Set<Long> assignedJudgeIds = judgeAssignments.stream()
                .map(StaffAssignment::getAccountId)
                .collect(Collectors.toSet());
        List<Integer> rounds = roundScores.stream()
                .map(RoundScore::getRoundNo)
                .distinct()
                .sorted()
                .collect(java.util.stream.Collectors.toList());
        if (bout.getCurrentRound() != null && bout.getCurrentRound() > 0) {
            rounds = java.util.stream.IntStream.rangeClosed(1, bout.getCurrentRound())
                    .boxed()
                    .collect(java.util.stream.Collectors.toList());
        }
        for (Integer roundNo : rounds) {
            Set<Long> submittedJudgeIds = roundScores.stream()
                    .filter(score -> roundNo.equals(score.getRoundNo()))
                    .filter(score -> score.getStatus() == RoundScoreStatus.SUBMITTED)
                    .map(RoundScore::getJudgeId)
                    .collect(Collectors.toSet());
            if (!assignedJudgeIds.equals(submittedJudgeIds)) {
                throw new WorkflowConflictException("SCORES_NOT_READY");
            }
        }
    }

    private void validateDecision(BoutResultConfirmRequest request) {
        validateDecision(request.getWinnerSide(), request.getDecisionType());
    }

    private void validateDecision(BoutSide winnerSide, DecisionType decisionType) {
        if (winnerSide == null || winnerSide == BoutSide.NONE) {
            throw new IllegalArgumentException("INVALID_WINNER_SELECTION");
        }
        if (decisionType == null || decisionType == DecisionType.UNKNOWN) {
            throw new IllegalArgumentException("INVALID_RESULT_DECISION");
        }
        if (winnerSide == BoutSide.DRAW && decisionType != DecisionType.POINTS) {
            throw new IllegalArgumentException("INVALID_WINNER_SELECTION");
        }
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
    }

    private void validateCorrectionRequest(BoutResultCorrectionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("result correction request is required");
        }
        if (request.getWinnerSide() == null) {
            throw new IllegalArgumentException("winnerSide is required");
        }
        if (request.getDecisionType() == null) {
            throw new IllegalArgumentException("decisionType is required");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("reason is required");
        }
        if (request.getReason().trim().length() > 500) {
            throw new IllegalArgumentException("reason must not exceed 500 characters");
        }
    }
}
