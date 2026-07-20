package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.domain.AuthSessionContext;
import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.domain.BoutEventType;
import com.boxing.bracket.event.dto.BoutEventResponse;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@Transactional
public class JudgeScoreService {

    private final BoutRepository boutRepository;
    private final RoundScoreRepository roundScoreRepository;
    private final BoutEventPublisher boutEventPublisher;
    private final StaffAssignmentService staffAssignmentService;

    public JudgeScoreService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            @Lazy BoutEventPublisher boutEventPublisher
    ) {
        this(boutRepository, roundScoreRepository, boutEventPublisher, null);
    }

    @Autowired
    public JudgeScoreService(
            BoutRepository boutRepository,
            RoundScoreRepository roundScoreRepository,
            @Lazy BoutEventPublisher boutEventPublisher,
            @Lazy StaffAssignmentService staffAssignmentService
    ) {
        this.boutRepository = boutRepository;
        this.roundScoreRepository = roundScoreRepository;
        this.boutEventPublisher = boutEventPublisher;
        this.staffAssignmentService = staffAssignmentService;
    }

    public RoundScoreResponse submitRoundScore(Long boutId, Integer roundNo, RoundScoreSubmitRequest request) {
        validatePathVariables(boutId, roundNo);
        validateRequest(request);
        validateScoreValues(request);

        Bout bout = boutRepository.findWithLockById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        Long judgeId = resolveJudgeId(request, boutId);
        bout.validateScoreSubmission(roundNo);

        RoundScore roundScore = roundScoreRepository
                .findByBoutIdAndRoundNoAndJudgeId(boutId, roundNo, judgeId)
                .orElseGet(() -> RoundScore.builder()
                        .boutId(boutId)
                        .roundNo(roundNo)
                        .judgeId(judgeId)
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
            throw new IllegalArgumentException("INVALID_ROUND_NUMBER");
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
        if (request.getJudgeId() == null && AuthSessionContext.get() == null) {
            throw new IllegalArgumentException("judgeId is required");
        }
        if (request.getRedScore() == null) {
            throw new IllegalArgumentException("redScore is required");
        }
        if (request.getBlueScore() == null) {
            throw new IllegalArgumentException("blueScore is required");
        }
    }

    private void validateScoreValues(RoundScoreSubmitRequest request) {
        if (request.getRedScore() < 0 || request.getBlueScore() < 0) {
            throw new IllegalArgumentException("INVALID_SCORE_VALUE");
        }
    }

    private Long resolveJudgeId(RoundScoreSubmitRequest request, Long boutId) {
        AuthSession session = AuthSessionContext.get();
        if (session == null) {
            if (request.getJudgeId() == null) {
                throw new IllegalArgumentException("judgeId is required");
            }
            return request.getJudgeId();
        }
        if (staffAssignmentService != null) {
            staffAssignmentService.requireBoutAccess(boutId);
        }
        if (request.getJudgeId() != null && !session.getAccountId().equals(request.getJudgeId())) {
            throw new AccessDeniedException("BOUT_ACCESS_DENIED");
        }
        return session.getAccountId();
    }
}
