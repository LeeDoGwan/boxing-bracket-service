package com.boxing.bracket.bout.admin.controller;

import com.boxing.bracket.bout.admin.dto.AdminBoutImportResponse;
import com.boxing.bracket.bout.admin.dto.AdminBoutRequest;
import com.boxing.bracket.bout.admin.dto.AdminBoutResponse;
import com.boxing.bracket.bout.admin.service.AdminBoutService;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminBoutController.class)
class AdminBoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminBoutService adminBoutService;

    @Test
    void getBoutsReturnsBoutList() throws Exception {
        given(adminBoutService.getBouts(1L)).willReturn(List.of(response(20L)));

        mockMvc.perform(get("/api/admin/bouts")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].boutId").value(20))
                .andExpect(jsonPath("$.data[0].boutNumber").value(1));
    }

    @Test
    void getBoutsReturnsBadRequestForMissingTournamentId() throws Exception {
        mockMvc.perform(get("/api/admin/bouts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    @Test
    void getBoutReturnsBout() throws Exception {
        given(adminBoutService.getBout(20L)).willReturn(response(20L));

        mockMvc.perform(get("/api/admin/bouts/{boutId}", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(20));
    }

    @Test
    void getBoutReturnsNotFoundForMissingBout() throws Exception {
        given(adminBoutService.getBout(99L)).willThrow(new BoutNotFoundException());

        mockMvc.perform(get("/api/admin/bouts/{boutId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    @Test
    void createBoutReturnsCreatedBout() throws Exception {
        AdminBoutRequest request = request();
        given(adminBoutService.createBout(any(AdminBoutRequest.class))).willReturn(response(20L));

        mockMvc.perform(post("/api/admin/bouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(20))
                .andExpect(jsonPath("$.data.redAthleteId").value(10))
                .andExpect(jsonPath("$.data.blueAthleteId").value(11));
    }

    @Test
    void createBoutReturnsBadRequestForMissingTournamentId() throws Exception {
        AdminBoutRequest request = new AdminBoutRequest(null, 1L, 1, "75", 10L, 11L, 3, 1, false);

        mockMvc.perform(post("/api/admin/bouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    @Test
    void createBoutReturnsNotFoundForMissingTournament() throws Exception {
        AdminBoutRequest request = request();
        given(adminBoutService.createBout(any(AdminBoutRequest.class)))
                .willThrow(new TournamentNotFoundException());

        mockMvc.perform(post("/api/admin/bouts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    @Test
    void importBoutsReturnsImportedCount() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "bouts.csv",
                "text/csv",
                "tournamentId,ringId,boutNumber,matchType,redAthleteId,blueAthleteId,totalRounds,scheduledOrder,eventBout\n"
                        .getBytes()
        );
        given(adminBoutService.importBouts(any())).willReturn(AdminBoutImportResponse.from(List.of(response(20L))));

        mockMvc.perform(multipart("/api/admin/bouts/import").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.importedCount").value(1))
                .andExpect(jsonPath("$.data.boutIds[0]").value(20));
    }

    @Test
    void updateBoutReturnsUpdatedBout() throws Exception {
        AdminBoutRequest request = request();
        given(adminBoutService.updateBout(eq(20L), any(AdminBoutRequest.class))).willReturn(response(20L));

        mockMvc.perform(put("/api/admin/bouts/{boutId}", 20L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.boutId").value(20))
                .andExpect(jsonPath("$.data.boutNumber").value(1));
    }

    @Test
    void updateBoutReturnsNotFoundForMissingBout() throws Exception {
        AdminBoutRequest request = request();
        given(adminBoutService.updateBout(eq(99L), any(AdminBoutRequest.class)))
                .willThrow(new BoutNotFoundException());

        mockMvc.perform(put("/api/admin/bouts/{boutId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    @Test
    void deleteBoutReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/bouts/{boutId}", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void deleteBoutReturnsNotFoundForMissingBout() throws Exception {
        willThrow(new BoutNotFoundException()).given(adminBoutService).deleteBout(99L);

        mockMvc.perform(delete("/api/admin/bouts/{boutId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bout not found"));
    }

    private AdminBoutRequest request() {
        return new AdminBoutRequest(1L, 1L, 1, "75 - middle school", 10L, 11L, 3, 1, false);
    }

    private AdminBoutResponse response(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .totalRounds(3)
                .scheduledOrder(1)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        return AdminBoutResponse.from(bout);
    }
}
