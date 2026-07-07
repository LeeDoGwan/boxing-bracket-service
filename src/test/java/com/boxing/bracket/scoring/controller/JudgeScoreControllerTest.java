package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.scoring.domain.RoundScore;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.service.JudgeScoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JudgeScoreController.class)
class JudgeScoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JudgeScoreService judgeScoreService;

    @Test
    void submitRoundScoreReturnsSubmittedScore() throws Exception {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);
        given(judgeScoreService.submitRoundScore(eq(1L), eq(1), any(RoundScoreSubmitRequest.class)))
                .willReturn(RoundScoreResponse.from(createSubmittedRoundScore()));

        mockMvc.perform(post("/api/judge/bouts/{boutId}/rounds/{roundNo}/scores", 1L, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(1))
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.redScore").value(10))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    @Test
    void submitRoundScoreReturnsBadRequestForInvalidScore() throws Exception {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, -1, 9);

        mockMvc.perform(post("/api/judge/bouts/{boutId}/rounds/{roundNo}/scores", 1L, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void submitRoundScoreReturnsNotFoundForMissingBout() throws Exception {
        RoundScoreSubmitRequest request = new RoundScoreSubmitRequest(10L, 10, 9);
        given(judgeScoreService.submitRoundScore(eq(99L), eq(1), any(RoundScoreSubmitRequest.class)))
                .willThrow(new BoutNotFoundException());

        mockMvc.perform(post("/api/judge/bouts/{boutId}/rounds/{roundNo}/scores", 99L, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    @Test
    void getBoutScoresReturnsScores() throws Exception {
        given(judgeScoreService.getBoutScores(1L))
                .willReturn(List.of(RoundScoreResponse.from(createSubmittedRoundScore())));

        mockMvc.perform(get("/api/judge/bouts/{boutId}/scores", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].boutId").value(1))
                .andExpect(jsonPath("$.data[0].roundNo").value(1))
                .andExpect(jsonPath("$.data[0].judgeId").value(10))
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"));
    }

    @Test
    void getBoutScoresReturnsNotFoundForMissingBout() throws Exception {
        given(judgeScoreService.getBoutScores(99L))
                .willThrow(new BoutNotFoundException());

        mockMvc.perform(get("/api/judge/bouts/{boutId}/scores", 99L))
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
