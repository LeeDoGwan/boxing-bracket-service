package com.boxing.bracket.operation.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.operation.dto.JudgeScoreSubmissionStatusResponse;
import com.boxing.bracket.operation.dto.OperationRingStatusResponse;
import com.boxing.bracket.operation.dto.TournamentOperationStatusResponse;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.domain.RoundScoreStatus;
import com.boxing.bracket.scoring.repository.RoundScoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TournamentOperationStatusServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private RingRepository ringRepository;

    @Mock
    private RoundScoreRepository roundScoreRepository;

    @InjectMocks
    private TournamentOperationStatusService tournamentOperationStatusService;

    @Test
    void getStatusRejectsNullTournamentId() {
        assertThatThrownBy(() -> tournamentOperationStatusService.getStatus(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    @Test
    void getStatusReturnsReadOnlyTournamentOperationSummary() {
        Bout previousBout = createBout(1L, 1L, 1, BoutStatus.SCHEDULED, false, false, null);
        Bout currentBout = createBout(
                2L,
                1L,
                2,
                BoutStatus.IN_PROGRESS,
                false,
                false,
                LocalDateTime.now().minusMinutes(16)
        );
        Bout nextBout = createBout(3L, 1L, 3, BoutStatus.READY, false, false, null);
        Bout pendingResultBout = createBout(4L, 2L, 1, BoutStatus.FINISHED, false, false, null);
        Bout confirmedBout = createBout(5L, 2L, 2, BoutStatus.FINISHED, true, false, null);
        Bout eventBout = createBout(6L, 1L, 4, BoutStatus.IN_PROGRESS, false, true, LocalDateTime.now().minusHours(1));
        Ring firstRing = createRing(1L, "Ring A", RingStatus.IN_PROGRESS, 2L);
        Ring secondRing = createRing(2L, "Ring B", RingStatus.READY, null);

        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(previousBout, currentBout, nextBout, pendingResultBout, confirmedBout, eventBout));
        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of(firstRing, secondRing));
        given(roundScoreRepository.findByBoutIdIn(anyList()))
                .willReturn(List.of(
                        createRoundScore(2L, 2, 100L, RoundScoreStatus.SUBMITTED),
                        createRoundScore(2L, 2, 101L, RoundScoreStatus.DRAFT)
                ));

        TournamentOperationStatusResponse response = tournamentOperationStatusService.getStatus(1L);

        assertThat(response.getTotalBoutCount()).isEqualTo(5);
        assertThat(response.getBoutStatusCounts())
                .containsEntry(BoutStatus.SCHEDULED, 1)
                .containsEntry(BoutStatus.READY, 1)
                .containsEntry(BoutStatus.IN_PROGRESS, 1)
                .containsEntry(BoutStatus.FINISHED, 2)
                .containsEntry(BoutStatus.SCORING, 0)
                .containsEntry(BoutStatus.CANCELED, 0);

        OperationRingStatusResponse firstRingStatus = response.getRings().get(0);
        assertThat(firstRingStatus.getCurrentBout().getBoutId()).isEqualTo(2L);
        assertThat(firstRingStatus.getCurrentBout().getCurrentRound()).isEqualTo(2);
        assertThat(firstRingStatus.getNextBout().getBoutId()).isEqualTo(3L);
        assertThat(response.getRings().get(1).getCurrentBout()).isNull();
        assertThat(response.getRings().get(1).getNextBout()).isNull();

        JudgeScoreSubmissionStatusResponse scoreStatus = response.getJudgeScoreSubmissionStatuses().get(0);
        assertThat(scoreStatus.getBoutId()).isEqualTo(2L);
        assertThat(scoreStatus.getRoundNo()).isEqualTo(2);
        assertThat(scoreStatus.getSubmittedJudgeIds()).containsExactly(100L);
        assertThat(scoreStatus.getUnsubmittedJudgeIds()).containsExactly(101L);
        assertThat(scoreStatus.isComplete()).isFalse();

        assertThat(response.getPendingResultBouts()).extracting("boutId").containsExactly(4L);
        assertThat(response.getStalledBouts()).extracting("boutId").containsExactly(2L);
        verify(boutRepository, never()).save(org.mockito.ArgumentMatchers.any(Bout.class));
        verify(ringRepository, never()).save(org.mockito.ArgumentMatchers.any(Ring.class));
    }

    private Bout createBout(
            Long id,
            Long ringId,
            int scheduledOrder,
            BoutStatus status,
            boolean resultConfirmed,
            boolean eventBout,
            LocalDateTime startedAt
    ) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(ringId)
                .boutNumber(scheduledOrder)
                .matchType("Elite")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(status)
                .currentRound(status == BoutStatus.IN_PROGRESS ? 2 : 0)
                .totalRounds(3)
                .scheduledOrder(scheduledOrder)
                .resultConfirmed(resultConfirmed)
                .eventBout(eventBout)
                .startedAt(startedAt)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Ring createRing(Long id, String name, RingStatus status, Long currentBoutId) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name(name)
                .status(status)
                .currentBoutId(currentBoutId)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }

    private RoundScore createRoundScore(Long boutId, int roundNo, Long judgeId, RoundScoreStatus status) {
        return RoundScore.builder()
                .boutId(boutId)
                .roundNo(roundNo)
                .judgeId(judgeId)
                .status(status)
                .build();
    }
}
