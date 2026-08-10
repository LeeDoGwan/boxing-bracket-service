package com.boxing.bracket.scoring.service;

import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.assignment.repository.StaffAssignmentRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.event.service.BoutEventPublisher;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SupervisorJudgeReadinessTest {

    @Mock
    private BoutRepository boutRepository;
    @Mock
    private RoundScoreRepository roundScoreRepository;
    @Mock
    private PenaltyRepository penaltyRepository;
    @Mock
    private BoutResultRepository boutResultRepository;
    @Mock
    private BoutEventPublisher boutEventPublisher;
    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private StaffAssignmentRepository staffAssignmentRepository;

    @Test
    void blocksResultConfirmationUntilAllThreeAssignedJudgesSubmit() {
        SupervisorResultService service = new SupervisorResultService(
                boutRepository,
                roundScoreRepository,
                penaltyRepository,
                boutResultRepository,
                boutEventPublisher,
                null,
                tournamentRepository,
                staffAssignmentRepository
        );
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(2L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.IN_PROGRESS)
                .currentRound(1)
                .build();
        given(boutRepository.findWithLockById(1L)).willReturn(Optional.of(bout));
        given(boutResultRepository.findByBoutId(1L)).willReturn(Optional.empty());
        given(tournamentRepository.findById(1L)).willReturn(Optional.of(Tournament.builder()
                .name("Seoul Cup")
                .judgeCount(3)
                .build()));
        given(staffAssignmentRepository.findByTournamentIdAndRingIdAndRoleAndActiveTrueOrderByAccountIdAsc(
                1L, 2L, UserRole.JUDGE
        )).willReturn(List.of(assignment(30L), assignment(31L), assignment(32L)));
        given(roundScoreRepository.findByBoutId(1L)).willReturn(List.of(
                submittedScore(30L), submittedScore(31L)
        ));

        assertThatThrownBy(() -> service.confirmResult(
                1L,
                new BoutResultConfirmRequest(BoutSide.RED, DecisionType.POINTS, 40L)
        ))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("SCORES_NOT_READY");
    }

    private StaffAssignment assignment(Long accountId) {
        return StaffAssignment.builder()
                .accountId(accountId)
                .tournamentId(1L)
                .ringId(2L)
                .role(UserRole.JUDGE)
                .build();
    }

    private RoundScore submittedScore(Long judgeId) {
        RoundScore score = RoundScore.builder()
                .boutId(1L)
                .roundNo(1)
                .judgeId(judgeId)
                .build();
        score.submit(10, 9);
        return score;
    }
}
