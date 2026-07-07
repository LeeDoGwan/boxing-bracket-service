package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.service.ScoreQueryService;
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

@WebMvcTest(SupervisorScoreController.class)
class SupervisorScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScoreQueryService scoreQueryService;

    @Test
    void getBoutScoresReturnsScores() throws Exception {
        given(scoreQueryService.getBoutScores(1L))
                .willReturn(List.of(RoundScoreResponse.from(createSubmittedRoundScore())));

        mockMvc.perform(get("/api/supervisor/bouts/{boutId}/scores", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].boutId").value(1))
                .andExpect(jsonPath("$.data[0].roundNo").value(1))
                .andExpect(jsonPath("$.data[0].judgeId").value(10))
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"));
    }

    @Test
    void getBoutScoresReturnsNotFoundForMissingBout() throws Exception {
        given(scoreQueryService.getBoutScores(99L))
                .willThrow(new BoutNotFoundException());

        mockMvc.perform(get("/api/supervisor/bouts/{boutId}/scores", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    private RoundScore createSubmittedRoundScore() {
        RoundScore roundScore = RoundScore.builder()
                .boutId(1L)
                .roundNo(1)
                .judgeId(10L)
                .build();
        ReflectionTestUtils.setField(roundScore, "id", 100L);
        roundScore.submit(10, 9);
        return roundScore;
    }
}
