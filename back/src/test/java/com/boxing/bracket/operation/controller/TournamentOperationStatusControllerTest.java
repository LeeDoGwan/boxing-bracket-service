package com.boxing.bracket.operation.controller;

import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.operation.dto.TournamentOperationStatusResponse;
import com.boxing.bracket.operation.service.TournamentOperationStatusService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TournamentOperationStatusController.class)
class TournamentOperationStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TournamentOperationStatusService tournamentOperationStatusService;

    @Test
    void getStatusReturnsTournamentOperationSummary() throws Exception {
        given(tournamentOperationStatusService.getStatus(1L))
                .willReturn(TournamentOperationStatusResponse.of(
                        1L,
                        4,
                        statusCounts(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(get("/api/admin/operations/status")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tournamentId").value(1))
                .andExpect(jsonPath("$.data.totalBoutCount").value(4))
                .andExpect(jsonPath("$.data.boutStatusCounts.IN_PROGRESS").value(1));
    }

    private Map<BoutStatus, Integer> statusCounts() {
        Map<BoutStatus, Integer> counts = new EnumMap<>(BoutStatus.class);
        for (BoutStatus status : BoutStatus.values()) {
            counts.put(status, status == BoutStatus.IN_PROGRESS ? 1 : 0);
        }
        return counts;
    }
}
