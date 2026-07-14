package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.domain.DecisionType;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.dto.BoutResultResponse;
import com.boxing.bracket.scoring.service.SupervisorResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupervisorResultController.class)
class SupervisorResultControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupervisorResultService supervisorResultService;

    @Test
    void confirmResultReturnsConfirmedResult() throws Exception {
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(BoutSide.RED, DecisionType.POINTS, 20L);
        given(supervisorResultService.confirmResult(eq(1L), any(BoutResultConfirmRequest.class)))
                .willReturn(BoutResultResponse.from(createBoutResult()));

        mockMvc.perform(post("/api/supervisor/bouts/{boutId}/result", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(1))
                .andExpect(jsonPath("$.data.redTotalScore").value(19))
                .andExpect(jsonPath("$.data.blueTotalScore").value(18))
                .andExpect(jsonPath("$.data.winnerSide").value("RED"))
                .andExpect(jsonPath("$.data.decisionType").value("POINTS"))
                .andExpect(jsonPath("$.data.confirmedBy").value(20));
    }

    @Test
    void confirmResultReturnsBadRequestForMissingWinnerSide() throws Exception {
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(null, DecisionType.POINTS, 20L);

        mockMvc.perform(post("/api/supervisor/bouts/{boutId}/result", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("winnerSide is required"));
    }

    @Test
    void confirmResultReturnsNotFoundForMissingBout() throws Exception {
        BoutResultConfirmRequest request = new BoutResultConfirmRequest(BoutSide.BLUE, DecisionType.RSC, 20L);
        given(supervisorResultService.confirmResult(eq(99L), any(BoutResultConfirmRequest.class)))
                .willThrow(new BoutNotFoundException());

        mockMvc.perform(post("/api/supervisor/bouts/{boutId}/result", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    private BoutResult createBoutResult() {
        BoutResult boutResult = BoutResult.builder()
                .boutId(1L)
                .redTotalScore(19)
                .blueTotalScore(18)
                .redPenaltyTotal(1)
                .bluePenaltyTotal(0)
                .winnerSide(BoutSide.RED)
                .decisionType(DecisionType.POINTS)
                .confirmedBy(20L)
                .confirmedAt(LocalDateTime.now())
                .build();
        ReflectionTestUtils.setField(boutResult, "id", 100L);
        return boutResult;
    }
}
