package com.boxing.bracket.tournament.admin.controller;

import com.boxing.bracket.tournament.admin.dto.AdminTournamentRequest;
import com.boxing.bracket.tournament.admin.dto.AdminTournamentResponse;
import com.boxing.bracket.tournament.admin.service.AdminTournamentService;
import com.boxing.bracket.tournament.domain.Tournament;
import com.boxing.bracket.tournament.domain.TournamentStatus;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminTournamentController.class)
class AdminTournamentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminTournamentService adminTournamentService;

    @Test
    void createTournamentReturnsCreatedTournament() throws Exception {
        AdminTournamentRequest request = request();
        given(adminTournamentService.createTournament(any(AdminTournamentRequest.class))).willReturn(response(1L));

        mockMvc.perform(post("/api/admin/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tournamentId").value(1))
                .andExpect(jsonPath("$.data.name").value("Seoul Cup"))
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void createTournamentReturnsBadRequestForBlankName() throws Exception {
        AdminTournamentRequest request = new AdminTournamentRequest(" ", "Seoul", null, null, null);

        mockMvc.perform(post("/api/admin/tournaments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    void updateTournamentReturnsUpdatedTournament() throws Exception {
        AdminTournamentRequest request = request();
        given(adminTournamentService.updateTournament(eq(1L), any(AdminTournamentRequest.class)))
                .willReturn(response(1L));

        mockMvc.perform(put("/api/admin/tournaments/{tournamentId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tournamentId").value(1));
    }

    @Test
    void updateTournamentReturnsNotFoundForMissingTournament() throws Exception {
        AdminTournamentRequest request = request();
        given(adminTournamentService.updateTournament(eq(99L), any(AdminTournamentRequest.class)))
                .willThrow(new TournamentNotFoundException());

        mockMvc.perform(put("/api/admin/tournaments/{tournamentId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    @Test
    void deleteTournamentReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/tournaments/{tournamentId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void deleteTournamentReturnsNotFoundForMissingTournament() throws Exception {
        willThrow(new TournamentNotFoundException()).given(adminTournamentService).deleteTournament(99L);

        mockMvc.perform(delete("/api/admin/tournaments/{tournamentId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    private AdminTournamentRequest request() {
        return new AdminTournamentRequest(
                "Seoul Cup",
                "Seoul",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 2),
                TournamentStatus.READY
        );
    }

    private AdminTournamentResponse response(Long id) {
        Tournament tournament = Tournament.builder()
                .name("Seoul Cup")
                .location("Seoul")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 2))
                .status(TournamentStatus.READY)
                .build();
        ReflectionTestUtils.setField(tournament, "id", id);
        return AdminTournamentResponse.from(tournament);
    }
}
