package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.dto.PenaltyCreateRequest;
import com.boxing.bracket.scoring.dto.PenaltyResponse;
import com.boxing.bracket.scoring.service.SupervisorPenaltyService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SupervisorPenaltyController.class)
class SupervisorPenaltyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SupervisorPenaltyService supervisorPenaltyService;

    @Test
    void getPenaltiesReturnsHistory() throws Exception {
        given(supervisorPenaltyService.getPenalties(1L))
                .willReturn(List.of(PenaltyResponse.from(createPenalty())));

        mockMvc.perform(get("/api/supervisor/bouts/{boutId}/penalties", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].penaltyId").value(100))
                .andExpect(jsonPath("$.data[0].targetSide").value("RED"));
    }

    @Test
    void createPenaltyReturnsCreatedPenalty() throws Exception {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 1, "warning", 20L);
        given(supervisorPenaltyService.createPenalty(eq(1L), any(PenaltyCreateRequest.class)))
                .willReturn(PenaltyResponse.from(createPenalty()));

        mockMvc.perform(post("/api/supervisor/bouts/{boutId}/penalties", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(1))
                .andExpect(jsonPath("$.data.targetSide").value("RED"))
                .andExpect(jsonPath("$.data.roundNo").value(1))
                .andExpect(jsonPath("$.data.penaltyPoint").value(1))
                .andExpect(jsonPath("$.data.createdBy").value(20));
    }

    @Test
    void createPenaltyReturnsBadRequestForMissingTargetSide() throws Exception {
        PenaltyCreateRequest request = new PenaltyCreateRequest(null, 1, "warning", 20L);

        mockMvc.perform(post("/api/supervisor/bouts/{boutId}/penalties", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("targetSide is required"));
    }

    @Test
    void createPenaltyReturnsNotFoundForMissingBout() throws Exception {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.BLUE, 1, "warning", 20L);
        given(supervisorPenaltyService.createPenalty(eq(99L), any(PenaltyCreateRequest.class)))
                .willThrow(new BoutNotFoundException());

        mockMvc.perform(post("/api/supervisor/bouts/{boutId}/penalties", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    private Penalty createPenalty() {
        Penalty penalty = Penalty.builder()
                .boutId(1L)
                .targetSide(BoutSide.RED)
                .roundNo(1)
                .penaltyPoint(1)
                .reason("warning")
                .createdBy(20L)
                .build();
        ReflectionTestUtils.setField(penalty, "id", 100L);
        return penalty;
    }
}
