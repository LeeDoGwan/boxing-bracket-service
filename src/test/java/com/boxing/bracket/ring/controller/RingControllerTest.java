package com.boxing.bracket.ring.controller;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.dto.RingBoutSummaryResponse;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.ring.service.RingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RingController.class)
class RingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RingService ringService;

    @Test
    void getRingStatusesReturnsRingCards() throws Exception {
        Ring ring = createRing(1L);
        RingStatusResponse response = RingStatusResponse.of(
                ring,
                RingBoutSummaryResponse.of(
                        createBout(10L, 3, 3, BoutStatus.IN_PROGRESS),
                        createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club"),
                        createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club")
                ),
                RingBoutSummaryResponse.of(
                        createBout(11L, 4, 4, BoutStatus.SCHEDULED),
                        createAthlete(12L, "Park Min Soo", "Seoul Boxing Club"),
                        createAthlete(13L, "Lee Jun Ho", "Busan Boxing Club")
                )
        );

        given(ringService.getRingStatuses(1L)).willReturn(List.of(response));

        mockMvc.perform(get("/api/rings/status")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ringId").value(1))
                .andExpect(jsonPath("$.data[0].currentBout.boutNumber").value(3))
                .andExpect(jsonPath("$.data[0].nextBout.boutNumber").value(4));
    }

    @Test
    void getRingStatusesReturnsBadRequestWithoutTournamentId() throws Exception {
        mockMvc.perform(get("/api/rings/status"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    @Test
    void getCurrentBoutReturnsBoutDetail() throws Exception {
        given(ringService.getCurrentBout(1L))
                .willReturn(BoutDetailResponse.of(
                        createBout(10L, 3, 3, BoutStatus.IN_PROGRESS),
                        createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club"),
                        createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club")
                ));

        mockMvc.perform(get("/api/rings/{ringId}/current-bout", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(10))
                .andExpect(jsonPath("$.data.redAthlete.name").value("Hong Gil Dong"));
    }

    @Test
    void getCurrentBoutReturnsNotFoundForMissingRing() throws Exception {
        given(ringService.getCurrentBout(99L)).willThrow(new RingNotFoundException());

        mockMvc.perform(get("/api/rings/{ringId}/current-bout", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ring not found"));
    }

    private Ring createRing(Long id) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring " + id)
                .status(RingStatus.IN_PROGRESS)
                .currentBoutId(10L)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }

    private Bout createBout(Long id, int boutNumber, int scheduledOrder, BoutStatus status) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(boutNumber)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(status)
                .currentRound(status == BoutStatus.IN_PROGRESS ? 1 : 0)
                .totalRounds(3)
                .scheduledOrder(scheduledOrder)
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
