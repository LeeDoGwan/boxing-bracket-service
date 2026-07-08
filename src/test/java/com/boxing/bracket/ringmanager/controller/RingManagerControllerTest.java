package com.boxing.bracket.ringmanager.controller;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import com.boxing.bracket.ringmanager.service.RingManagerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RingManagerController.class)
class RingManagerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RingManagerService ringManagerService;

    @Test
    void startBoutReturnsStartedBout() throws Exception {
        Bout bout = createBout(10L);
        bout.start();
        given(ringManagerService.startBout(10L)).willReturn(RingManagerBoutResponse.from(bout));

        mockMvc.perform(post("/api/ring-manager/bouts/{boutId}/start", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(10))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void startBoutReturnsNotFoundForMissingBout() throws Exception {
        given(ringManagerService.startBout(99L)).willThrow(new BoutNotFoundException());

        mockMvc.perform(post("/api/ring-manager/bouts/{boutId}/start", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    private Bout createBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.READY)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return bout;
    }
}
