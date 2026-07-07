package com.boxing.bracket.bout.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
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
class BoutServiceTest {

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private AthleteRepository athleteRepository;

    @InjectMocks
    private BoutService boutService;

    @Test
    void getOfficialBoutsExcludesEventBoutAndIncludesAthletes() {
        Bout officialBout = createBout(1L, 1, 1, 10L, 11L, false);
        Bout eventBout = createBout(2L, 2, 2, 12L, 13L, true);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(officialBout, eventBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.getOfficialBouts(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBoutNumber()).isEqualTo(1);
        assertThat(responses.get(0).getRedAthlete().getName()).isEqualTo("Hong Gil Dong");
        assertThat(responses.get(0).getRedAthlete().getAffiliation()).isEqualTo("Incheon Boxing Club");
        assertThat(responses.get(0).getBlueAthlete().getName()).isEqualTo("Kim Chul Soo");
        assertThat(responses.get(0).getBlueAthlete().getAffiliation()).isEqualTo("Seoul Boxing Club");
        verify(athleteRepository, never()).findById(eq(12L));
        verify(athleteRepository, never()).findById(eq(13L));
    }

    @Test
    void getOfficialBoutsRejectsNullTournamentId() {
        assertThatThrownBy(() -> boutService.getOfficialBouts(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    @Test
    void getBoutDetailReturnsBoutAndAthletes() {
        Bout bout = createBout(1L, 1, 1, 10L, 11L, false);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(boutRepository.findById(1L)).willReturn(Optional.of(bout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        BoutDetailResponse response = boutService.getBoutDetail(1L);

        assertThat(response.getBoutId()).isEqualTo(1L);
        assertThat(response.getTournamentId()).isEqualTo(1L);
        assertThat(response.getBoutNumber()).isEqualTo(1);
        assertThat(response.getRedAthlete().getName()).isEqualTo("Hong Gil Dong");
        assertThat(response.getBlueAthlete().getAffiliation()).isEqualTo("Seoul Boxing Club");
    }

    @Test
    void getBoutDetailRejectsMissingBout() {
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> boutService.getBoutDetail(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    private Bout createBout(
            Long id,
            int boutNumber,
            int scheduledOrder,
            Long redAthleteId,
            Long blueAthleteId,
            boolean eventBout
    ) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(boutNumber)
                .matchType("75 - middle school")
                .redAthleteId(redAthleteId)
                .blueAthleteId(blueAthleteId)
                .status(BoutStatus.SCHEDULED)
                .currentRound(0)
                .totalRounds(3)
                .scheduledOrder(scheduledOrder)
                .eventBout(eventBout)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }

    private Athlete createAthlete(Long id, String name, String affiliation) {
        Athlete athlete = Athlete.builder()
                .name(name)
                .affiliation(affiliation)
                .build();
        ReflectionTestUtils.setField(athlete, "id", id);
        return athlete;
    }
}
