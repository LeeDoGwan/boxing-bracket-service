package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.dto.BoutResultResponse;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SupervisorResultServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RoundScoreRepository roundScoreRepository;

    @Mock
    private PenaltyRepository penaltyRepository;

    @Mock
    private BoutResultRepository boutResultRepository;

    @InjectMocks
    private SupervisorResultService supervisorResultService;

    @Test
    void confirmResultCreatesResultFromSubmittedScoresAndPenalties() {
        Bout bout = createBout();
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(BoutSide.RED, DecisionType.POINTS, 20L);
        given(boutRepository.findById(1L)).willReturn(Optional.of(bout));
        given(roundScoreRepository.findByBoutId(1L))
                .willReturn(List.of(
                        createSubmittedRoundScore(1L, 1, 10L, 10, 9),
                        createSubmittedRoundScore(1L, 2, 11L, 9, 10)
                ));
        given(penaltyRepository.findByBoutId(1L))
                .willReturn(List.of(
                        createPenalty(BoutSide.RED, 1),
                        createPenalty(BoutSide.BLUE, 2)
                ));
        given(boutResultRepository.findByBoutId(1L)).willReturn(Optional.empty());
        given(boutResultRepository.save(any(BoutResult.class))).willAnswer(invocation -> {
            BoutResult boutResult = invocation.getArgument(0);
            ReflectionTestUtils.setField(boutResult, "id", 100L);
            return boutResult;
        });

        BoutResultResponse response = supervisorResultService.confirmResult(1L, request);

        assertThat(response.getResultId()).isEqualTo(100L);
        assertThat(response.getBoutId()).isEqualTo(1L);
        assertThat(response.getRedTotalScore()).isEqualTo(19);
        assertThat(response.getBlueTotalScore()).isEqualTo(19);
        assertThat(response.getRedPenaltyTotal()).isEqualTo(1);
        assertThat(response.getBluePenaltyTotal()).isEqualTo(2);
        assertThat(response.getWinnerSide()).isEqualTo(BoutSide.RED);
        assertThat(response.getDecisionType()).isEqualTo(DecisionType.POINTS);
        assertThat(response.getConfirmedBy()).isEqualTo(20L);
        assertThat(response.getConfirmedAt()).isNotNull();
        assertThat(bout.getStatus()).isEqualTo(BoutStatus.FINISHED);
        assertThat(bout.isResultConfirmed()).isTrue();
    }

    @Test
    void confirmResultUpdatesExistingResult() {
        Bout bout = createBout();
        BoutResult existingResult = BoutResult.builder().boutId(1L).build();
        ReflectionTestUtils.setField(existingResult, "id", 200L);
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(BoutSide.BLUE, DecisionType.RSC, 21L);
        given(boutRepository.findById(1L)).willReturn(Optional.of(bout));
        given(roundScoreRepository.findByBoutId(1L)).willReturn(List.of(createSubmittedRoundScore(1L, 1, 10L, 8, 10)));
        given(penaltyRepository.findByBoutId(1L)).willReturn(List.of());
        given(boutResultRepository.findByBoutId(1L)).willReturn(Optional.of(existingResult));
        given(boutResultRepository.save(any(BoutResult.class))).willAnswer(invocation -> invocation.getArgument(0));

        BoutResultResponse response = supervisorResultService.confirmResult(1L, request);

        assertThat(response.getResultId()).isEqualTo(200L);
        assertThat(response.getWinnerSide()).isEqualTo(BoutSide.BLUE);
        assertThat(response.getDecisionType()).isEqualTo(DecisionType.RSC);
        assertThat(response.getRedTotalScore()).isEqualTo(8);
        assertThat(response.getBlueTotalScore()).isEqualTo(10);
    }

    @Test
    void confirmResultRejectsMissingBout() {
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(BoutSide.RED, DecisionType.POINTS, 20L);
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> supervisorResultService.confirmResult(99L, request))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void confirmResultRejectsNullWinnerSide() {
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(null, DecisionType.POINTS, 20L);

        assertThatThrownBy(() -> supervisorResultService.confirmResult(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("winnerSide is required");
    }

    private Bout createBout() {
        return Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.IN_PROGRESS)
                .build();
    }

    private RoundScore createSubmittedRoundScore(Long boutId, Integer roundNo, Long judgeId, int redScore, int blueScore) {
        RoundScore roundScore = RoundScore.builder()
                .boutId(boutId)
                .roundNo(roundNo)
                .judgeId(judgeId)
                .build();
        roundScore.submit(redScore, blueScore);
        return roundScore;
    }

    private Penalty createPenalty(BoutSide targetSide, Integer penaltyPoint) {
        return Penalty.builder()
                .boutId(1L)
                .targetSide(targetSide)
                .penaltyPoint(penaltyPoint)
                .reason("warning")
                .createdBy(20L)
                .build();
    }
}
