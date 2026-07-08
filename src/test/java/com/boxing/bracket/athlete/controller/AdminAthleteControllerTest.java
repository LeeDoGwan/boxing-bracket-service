package com.boxing.bracket.athlete.controller;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.dto.AthleteRequest;
import com.boxing.bracket.athlete.dto.AthleteResponse;
import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.service.AdminAthleteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAthleteController.class)
class AdminAthleteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminAthleteService adminAthleteService;

    @Test
    void createAthleteReturnsCreatedAthlete() throws Exception {
        AthleteRequest request = new AthleteRequest("Kim Min", "Blue Gym");
        given(adminAthleteService.createAthlete(any(AthleteRequest.class)))
                .willReturn(createResponse(10L, "Kim Min", "Blue Gym"));

        mockMvc.perform(post("/api/admin/athletes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.athleteId").value(10))
                .andExpect(jsonPath("$.data.name").value("Kim Min"))
                .andExpect(jsonPath("$.data.affiliation").value("Blue Gym"));
    }

    @Test
    void createAthleteReturnsBadRequestForBlankName() throws Exception {
        AthleteRequest request = new AthleteRequest(" ", "Blue Gym");

        mockMvc.perform(post("/api/admin/athletes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("name is required"));
    }

    @Test
    void updateAthleteReturnsUpdatedAthlete() throws Exception {
        AthleteRequest request = new AthleteRequest("Lee Jun", "Red Gym");
        given(adminAthleteService.updateAthlete(eq(10L), any(AthleteRequest.class)))
                .willReturn(createResponse(10L, "Lee Jun", "Red Gym"));

        mockMvc.perform(put("/api/admin/athletes/{athleteId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.athleteId").value(10))
                .andExpect(jsonPath("$.data.name").value("Lee Jun"))
                .andExpect(jsonPath("$.data.affiliation").value("Red Gym"));
    }

    @Test
    void updateAthleteReturnsNotFoundForMissingAthlete() throws Exception {
        AthleteRequest request = new AthleteRequest("Lee Jun", "Red Gym");
        given(adminAthleteService.updateAthlete(eq(99L), any(AthleteRequest.class)))
                .willThrow(new AthleteNotFoundException());

        mockMvc.perform(put("/api/admin/athletes/{athleteId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Athlete not found"));
    }

    @Test
    void deleteAthleteReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/athletes/{athleteId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void deleteAthleteReturnsNotFoundForMissingAthlete() throws Exception {
        willThrow(new AthleteNotFoundException()).given(adminAthleteService).deleteAthlete(99L);

        mockMvc.perform(delete("/api/admin/athletes/{athleteId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Athlete not found"));
    }

    private AthleteResponse createResponse(Long id, String name, String affiliation) {
        Athlete athlete = Athlete.builder()
                .name(name)
                .affiliation(affiliation)
                .build();
        ReflectionTestUtils.setField(athlete, "id", id);
        return AthleteResponse.from(athlete);
    }
}
