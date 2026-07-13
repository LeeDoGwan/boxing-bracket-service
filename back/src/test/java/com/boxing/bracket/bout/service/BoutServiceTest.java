package com.boxing.bracket.bout.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
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

    @Mock
    private BoutResultRepository boutResultRepository;

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
    void getOfficialBoutsIncludesConfirmedResult() {
        Bout bout = createBout(1L, 1, 1, 10L, 11L, false);
        bout.finish(BoutSide.RED);
        bout.confirmResult(BoutSide.RED);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");
        BoutResult boutResult = createBoutResult(100L, 1L, BoutSide.RED);

        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(bout));
        given(boutResultRepository.findByBoutIdIn(anyCollection()))
                .willReturn(List.of(boutResult));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.getOfficialBouts(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isResultConfirmed()).isTrue();
        assertThat(responses.get(0).getWinnerSide()).isEqualTo(BoutSide.RED);
        assertThat(responses.get(0).getResult().getResultId()).isEqualTo(100L);
        assertThat(responses.get(0).getResult().getRedTotalScore()).isEqualTo(19);
        assertThat(responses.get(0).getResult().getDecisionType()).isEqualTo(DecisionType.POINTS);
    }

    @Test
    void searchOfficialBoutsFindsBoutByAthleteName() {
        Bout matchedBout = createBout(1L, 1, 1, 10L, 11L, false);
        Bout unmatchedBout = createBout(2L, 2, 2, 12L, 13L, false);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(athleteRepository.findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase("Hong", "Hong"))
                .willReturn(List.of(redAthlete));
        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(matchedBout, unmatchedBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.searchOfficialBouts(1L, "Hong");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBoutId()).isEqualTo(1L);
        assertThat(responses.get(0).getRedAthlete().getName()).isEqualTo("Hong Gil Dong");
    }

    @Test
    void searchOfficialBoutsFindsBoutByAthleteAffiliation() {
        Bout matchedBout = createBout(1L, 1, 1, 10L, 11L, false);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(athleteRepository.findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase("Seoul", "Seoul"))
                .willReturn(List.of(blueAthlete));
        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(matchedBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.searchOfficialBouts(1L, "Seoul");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBlueAthlete().getAffiliation()).isEqualTo("Seoul Boxing Club");
    }

    @Test
    void searchOfficialBoutsFindsBoutByMatchType() {
        Bout matchedBout = createBout(1L, 1, 1, 10L, 11L, "75 - middle school", false);
        Bout unmatchedBout = createBout(2L, 2, 2, 12L, 13L, "80 - high school", false);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(athleteRepository.findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase("75", "75"))
                .willReturn(List.of());
        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(matchedBout, unmatchedBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.searchOfficialBouts(1L, "75");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getMatchType()).isEqualTo("75 - middle school");
    }

    @Test
    void searchOfficialBoutsFindsBoutByBoutNumber() {
        Bout firstBout = createBout(1L, 1, 1, 10L, 11L, "75 - middle school", false);
        Bout secondBout = createBout(2L, 2, 2, 12L, 13L, "80 - high school", false);
        Athlete redAthlete = createAthlete(12L, "Park Min Jae", "Busan Boxing Club");
        Athlete blueAthlete = createAthlete(13L, "Lee Ji Hoon", "Daegu Boxing Club");

        given(athleteRepository.findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase("2", "2"))
                .willReturn(List.of());
        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(firstBout, secondBout));
        given(athleteRepository.findById(12L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(13L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.searchOfficialBouts(1L, "2");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBoutNumber()).isEqualTo(2);
    }

    @Test
    void searchOfficialBoutsExcludesEventBout() {
        Bout officialBout = createBout(1L, 1, 1, 10L, 11L, "75 - middle school", false);
        Bout eventBout = createBout(2L, 2, 2, 12L, 13L, "75 - middle school", true);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(athleteRepository.findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase("75", "75"))
                .willReturn(List.of());
        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(officialBout, eventBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.searchOfficialBouts(1L, "75");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBoutId()).isEqualTo(1L);
        verify(athleteRepository, never()).findById(eq(12L));
        verify(athleteRepository, never()).findById(eq(13L));
    }

    @Test
    void searchOfficialBoutsReturnsOfficialBoutsWhenKeywordIsBlank() {
        Bout officialBout = createBout(1L, 1, 1, 10L, 11L, false);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(boutRepository.findByTournamentIdOrderByScheduledOrderAsc(1L))
                .willReturn(List.of(officialBout));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        List<BoutListResponse> responses = boutService.searchOfficialBouts(1L, "   ");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getBoutId()).isEqualTo(1L);
        verify(athleteRepository, never())
                .findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase("   ", "   ");
    }

    @Test
    void searchOfficialBoutsRejectsNullTournamentId() {
        assertThatThrownBy(() -> boutService.searchOfficialBouts(null, "Hong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("tournamentId is required");
    }

    @Test
    void getBoutDetailReturnsBoutAndAthletes() {
        Bout bout = createBout(1L, 1, 1, 10L, 11L, false);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");

        given(boutRepository.findById(1L)).willReturn(Optional.of(bout));
        given(boutResultRepository.findByBoutId(1L)).willReturn(Optional.empty());
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
    void getBoutDetailIncludesConfirmedResult() {
        Bout bout = createBout(1L, 1, 1, 10L, 11L, false);
        bout.finish(BoutSide.BLUE);
        bout.confirmResult(BoutSide.BLUE);
        Athlete redAthlete = createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club");
        Athlete blueAthlete = createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club");
        BoutResult boutResult = createBoutResult(100L, 1L, BoutSide.BLUE);

        given(boutRepository.findById(1L)).willReturn(Optional.of(bout));
        given(boutResultRepository.findByBoutId(1L)).willReturn(Optional.of(boutResult));
        given(athleteRepository.findById(10L)).willReturn(Optional.of(redAthlete));
        given(athleteRepository.findById(11L)).willReturn(Optional.of(blueAthlete));

        BoutDetailResponse response = boutService.getBoutDetail(1L);

        assertThat(response.isResultConfirmed()).isTrue();
        assertThat(response.getWinnerSide()).isEqualTo(BoutSide.BLUE);
        assertThat(response.getResult().getResultId()).isEqualTo(100L);
        assertThat(response.getResult().getWinnerSide()).isEqualTo(BoutSide.BLUE);
        assertThat(response.getResult().getBlueTotalScore()).isEqualTo(18);
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
        return createBout(id, boutNumber, scheduledOrder, redAthleteId, blueAthleteId, "75 - middle school", eventBout);
    }

    private Bout createBout(
            Long id,
            int boutNumber,
            int scheduledOrder,
            Long redAthleteId,
            Long blueAthleteId,
            String matchType,
            boolean eventBout
    ) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(boutNumber)
                .matchType(matchType)
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

    private BoutResult createBoutResult(Long id, Long boutId, BoutSide winnerSide) {
        BoutResult boutResult = BoutResult.builder()
                .boutId(boutId)
                .redTotalScore(19)
                .blueTotalScore(18)
                .redPenaltyTotal(1)
                .bluePenaltyTotal(0)
                .winnerSide(winnerSide)
                .decisionType(DecisionType.POINTS)
                .confirmedBy(20L)
                .build();
        ReflectionTestUtils.setField(boutResult, "id", id);
        return boutResult;
    }
}
