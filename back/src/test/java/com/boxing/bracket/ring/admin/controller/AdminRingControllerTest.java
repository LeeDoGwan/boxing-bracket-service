package com.boxing.bracket.ring.admin.controller;

import com.boxing.bracket.ring.admin.dto.AdminRingRequest;
import com.boxing.bracket.ring.admin.dto.AdminRingResponse;
import com.boxing.bracket.ring.admin.service.AdminRingService;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
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
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminRingController.class)
class AdminRingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminRingService adminRingService;

    @Test
    void getRingsReturnsRingList() throws Exception {
        given(adminRingService.getRings(1L)).willReturn(List.of(response(10L)));

        mockMvc.perform(get("/api/admin/rings")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].ringId").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Ring 1"));
    }

    @Test
    void getRingsReturnsBadRequestForMissingTournamentId() throws Exception {
        mockMvc.perform(get("/api/admin/rings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    @Test
    void getRingReturnsRing() throws Exception {
        given(adminRingService.getRing(10L)).willReturn(response(10L));

        mockMvc.perform(get("/api/admin/rings/{ringId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ringId").value(10));
    }

    @Test
    void getRingReturnsNotFoundForMissingRing() throws Exception {
        given(adminRingService.getRing(99L)).willThrow(new RingNotFoundException());

        mockMvc.perform(get("/api/admin/rings/{ringId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ring not found"));
    }

    @Test
    void createRingReturnsCreatedRing() throws Exception {
        AdminRingRequest request = new AdminRingRequest(1L, "Ring 1", null);
        given(adminRingService.createRing(any(AdminRingRequest.class))).willReturn(response(10L));

        mockMvc.perform(post("/api/admin/rings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ringId").value(10))
                .andExpect(jsonPath("$.data.name").value("Ring 1"))
                .andExpect(jsonPath("$.data.status").value("READY"));
    }

    @Test
    void createRingReturnsBadRequestForBlankName() throws Exception {
        AdminRingRequest request = new AdminRingRequest(1L, " ", null);

        mockMvc.perform(post("/api/admin/rings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    void createRingReturnsNotFoundForMissingTournament() throws Exception {
        AdminRingRequest request = new AdminRingRequest(99L, "Ring 1", null);
        given(adminRingService.createRing(any(AdminRingRequest.class)))
                .willThrow(new TournamentNotFoundException());

        mockMvc.perform(post("/api/admin/rings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    @Test
    void updateRingReturnsUpdatedRing() throws Exception {
        AdminRingRequest request = new AdminRingRequest(1L, "Ring 1", RingStatus.READY);
        given(adminRingService.updateRing(eq(10L), any(AdminRingRequest.class))).willReturn(response(10L));

        mockMvc.perform(put("/api/admin/rings/{ringId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.ringId").value(10));
    }

    @Test
    void updateRingReturnsNotFoundForMissingRing() throws Exception {
        AdminRingRequest request = new AdminRingRequest(1L, "Ring 1", RingStatus.READY);
        given(adminRingService.updateRing(eq(99L), any(AdminRingRequest.class)))
                .willThrow(new RingNotFoundException());

        mockMvc.perform(put("/api/admin/rings/{ringId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ring not found"));
    }

    @Test
    void deleteRingReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/rings/{ringId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void deleteRingReturnsNotFoundForMissingRing() throws Exception {
        willThrow(new RingNotFoundException()).given(adminRingService).deleteRing(99L);

        mockMvc.perform(delete("/api/admin/rings/{ringId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Ring not found"));
    }

    private AdminRingResponse response(Long id) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring 1")
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return AdminRingResponse.from(ring);
    }
}
