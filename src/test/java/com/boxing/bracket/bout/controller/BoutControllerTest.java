package com.boxing.bracket.bout.controller;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.service.BoutService;
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

@WebMvcTest(BoutController.class)
class BoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoutService boutService;

    @Test
    void getOfficialBoutsReturnsBoutList() throws Exception {
        given(boutService.getOfficialBouts(1L))
                .willReturn(List.of(BoutListResponse.of(
                        createBout(1L, 1, 1, false),
                        createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club"),
                        createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club")
                )));

        mockMvc.perform(get("/api/bouts")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].boutNumber").value(1))
                .andExpect(jsonPath("$.data[0].redAthlete.name").value("Hong Gil Dong"));
    }

    @Test
    void searchOfficialBoutsReturnsBoutListByAthleteKeyword() throws Exception {
        given(boutService.searchOfficialBouts(1L, "홍길동"))
                .willReturn(List.of(BoutListResponse.of(
                        createBout(1L, 1, 1, false),
                        createAthlete(10L, "홍길동", "Incheon Boxing Club"),
                        createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club")
                )));

        mockMvc.perform(get("/api/bouts/search")
                        .param("tournamentId", "1")
                        .param("keyword", "홍길동"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].redAthlete.name").value("홍길동"));
    }

    @Test
    void searchOfficialBoutsReturnsBoutListByMatchTypeKeyword() throws Exception {
        given(boutService.searchOfficialBouts(1L, "75"))
                .willReturn(List.of(BoutListResponse.of(
                        createBout(1L, 1, 1, false),
                        createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club"),
                        createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club")
                )));

        mockMvc.perform(get("/api/bouts/search")
                        .param("tournamentId", "1")
                        .param("keyword", "75"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].matchType").value("75 - middle school"));
    }

    @Test
    void searchOfficialBoutsReturnsBadRequestWithoutTournamentId() throws Exception {
        mockMvc.perform(get("/api/bouts/search")
                        .param("keyword", "홍길동"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    @Test
    void getBoutDetailReturnsBoutDetail() throws Exception {
        given(boutService.getBoutDetail(1L))
                .willReturn(BoutDetailResponse.of(
                        createBout(1L, 1, 1, false),
                        createAthlete(10L, "Hong Gil Dong", "Incheon Boxing Club"),
                        createAthlete(11L, "Kim Chul Soo", "Seoul Boxing Club")
                ));

        mockMvc.perform(get("/api/bouts/{boutId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(1));
    }

    @Test
    void getBoutDetailReturnsNotFoundForMissingBout() throws Exception {
        given(boutService.getBoutDetail(99L)).willThrow(new BoutNotFoundException());

        mockMvc.perform(get("/api/bouts/{boutId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    private Bout createBout(Long id, int boutNumber, int scheduledOrder, boolean eventBout) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(boutNumber)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
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
