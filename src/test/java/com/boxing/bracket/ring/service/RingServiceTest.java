package com.boxing.bracket.ring.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.ring.repository.RingRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RingServiceTest {

    @Mock
    private RingRepository ringRepository;

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private RingService ringService;

    @Test
    void getRingStatusesRejectsNullTournamentId() {
        assertThatThrownBy(() -> ringService.getRingStatuses(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    @Test
    void getRingStatusesReturnsEmptyListWhenRingsDoNotExist() {
        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of());

        List<RingStatusResponse> responses = ringService.getRingStatuses(1L);

        assertThat(responses).isEmpty();
    }

    @Test
    void getRingStatusesUsesRingCurrentBoutIdAsCurrentBout() {
        Ring ring = createRing(1L, RingStatus.IN_PROGRESS, 10L);
        Bout currentBout = createBout(10L, 3, 3, 10L, 11L, BoutStatus.SCHEDULED, false);
        Bout slowerNextBout = createBout(12L, 5, 5, 14L, 15L, BoutStatus.SCHEDULED, false);
        Bout nextBout = createBout(11L, 4, 4, 12L, 13L, BoutStatus.SCHEDULED, false);

        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(currentBout, slowerNextBout, nextBout));
        givenAthletes(10L, 11L, 12L, 13L);

        RingStatusResponse response = ringService.getRingStatuses(1L).get(0);

        assertThat(response.getRingId()).isEqualTo(1L);
        assertThat(response.getCurrentBout().getBoutNumber()).isEqualTo(3);
        assertThat(response.getNextBout().getBoutNumber()).isEqualTo(4);
        assertThat(response.getCurrentBout().getRedAthleteName()).isEqualTo("Athlete 10");
        assertThat(response.getCurrentBout().getBlueAthleteAffiliation()).isEqualTo("Club 11");
    }

    @Test
    void getRingStatusesUsesInProgressBoutWhenCurrentBoutIdIsMissing() {
        Ring ring = createRing(1L, RingStatus.IN_PROGRESS, null);
        Bout previousBout = createBout(9L, 2, 2, 8L, 9L, BoutStatus.FINISHED, false);
        Bout currentBout = createBout(10L, 3, 3, 10L, 11L, BoutStatus.IN_PROGRESS, false);
        Bout nextBout = createBout(11L, 4, 4, 12L, 13L, BoutStatus.READY, false);

        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(previousBout, currentBout, nextBout));
        givenAthletes(10L, 11L, 12L, 13L);

        RingStatusResponse response = ringService.getRingStatuses(1L).get(0);

        assertThat(response.getCurrentBout().getBoutId()).isEqualTo(10L);
        assertThat(response.getCurrentBout().getBoutStatus()).isEqualTo(BoutStatus.IN_PROGRESS);
        assertThat(response.getNextBout().getBoutId()).isEqualTo(11L);
    }

    @Test
    void getRingStatusesUsesFirstScheduledBoutAsNextWhenCurrentBoutDoesNotExist() {
        Ring ring = createRing(1L, RingStatus.READY, null);
        Bout laterBout = createBout(12L, 5, 5, 14L, 15L, BoutStatus.SCHEDULED, false);
        Bout nextBout = createBout(11L, 4, 4, 12L, 13L, BoutStatus.READY, false);

        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(laterBout, nextBout));
        givenAthletes(12L, 13L);

        RingStatusResponse response = ringService.getRingStatuses(1L).get(0);

        assertThat(response.getCurrentBout()).isNull();
        assertThat(response.getNextBout().getBoutNumber()).isEqualTo(4);
    }

    @Test
    void getRingStatusesExcludesEventBoutsFromCurrentAndNextCandidates() {
        Ring ring = createRing(1L, RingStatus.IN_PROGRESS, 10L);
        Bout eventBout = createBout(10L, 3, 3, 10L, 11L, BoutStatus.IN_PROGRESS, true);
        Bout nextBout = createBout(11L, 4, 4, 12L, 13L, BoutStatus.SCHEDULED, false);

        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(eventBout, nextBout));
        givenAthletes(12L, 13L);

        RingStatusResponse response = ringService.getRingStatuses(1L).get(0);

        assertThat(response.getCurrentBout()).isNull();
        assertThat(response.getNextBout().getBoutId()).isEqualTo(11L);
        verify(athleteRepository, never()).findById(eq(10L));
        verify(athleteRepository, never()).findById(eq(11L));
    }

    @Test
    void getRingStatusesRejectsMissingAthlete() {
        Ring ring = createRing(1L, RingStatus.IN_PROGRESS, 10L);
        Bout currentBout = createBout(10L, 3, 3, 10L, 11L, BoutStatus.IN_PROGRESS, false);

        given(ringRepository.findByTournamentIdOrderByIdAsc(1L)).willReturn(List.of(ring));
        given(boutRepository.findByRingIdOrderByScheduledOrderAsc(1L)).willReturn(List.of(currentBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ringService.getRingStatuses(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Athlete not found");
    }

    private void givenAthletes(Long... athleteIds) {
        for (Long athleteId : athleteIds) {
            given(athleteRepository.findById(athleteId))
                    .willReturn(Optional.of(createAthlete(athleteId)));
        }
    }

    private Ring createRing(Long id, RingStatus status, Long currentBoutId) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring " + id)
                .status(status)
                .currentBoutId(currentBoutId)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }

    private Bout createBout(
            Long id,
            int boutNumber,
            int scheduledOrder,
            Long redAthleteId,
            Long blueAthleteId,
            BoutStatus status,
            boolean eventBout
    ) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(boutNumber)
                .matchType("75 - middle school")
                .redAthleteId(redAthleteId)
                .blueAthleteId(blueAthleteId)
                .status(status)
                .currentRound(status == BoutStatus.IN_PROGRESS ? 1 : 0)
                .totalRounds(3)
                .scheduledOrder(scheduledOrder)
                .eventBout(eventBout)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Athlete createAthlete(Long id) {
        Athlete athlete = Athlete.builder()
                .name("Athlete " + id)
                .affiliation("Club " + id)
                .build();
        ReflectionTestUtils.setField(athlete, "id", id);
        return athlete;
    }
}
