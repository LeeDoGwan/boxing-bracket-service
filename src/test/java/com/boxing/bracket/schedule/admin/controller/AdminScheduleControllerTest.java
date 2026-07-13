package com.boxing.bracket.schedule.admin.controller;

import com.boxing.bracket.schedule.admin.dto.ScheduleRequest;
import com.boxing.bracket.schedule.admin.service.AdminScheduleService;
import com.boxing.bracket.schedule.domain.ScheduleItem;
import com.boxing.bracket.schedule.domain.ScheduleStatus;
import com.boxing.bracket.schedule.domain.ScheduleType;
import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminScheduleController.class)
class AdminScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminScheduleService adminScheduleService;

    @Test
    void getSchedulesReturnsTournamentScheduleList() throws Exception {
        given(adminScheduleService.getSchedules(1L)).willReturn(List.of(response(5L)));

        mockMvc.perform(get("/api/admin/schedules").param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].scheduleId").value(5));
    }

    @Test
    void createScheduleAcceptsSchedulePayload() throws Exception {
        given(adminScheduleService.createSchedule(any(ScheduleRequest.class))).willReturn(response(5L));

        mockMvc.perform(post("/api/admin/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Opening"));
    }

    @Test
    void createScheduleRequiresStartTime() throws Exception {
        ScheduleRequest request = new ScheduleRequest(
                1L, null, ScheduleType.EVENT, "Opening", null, null, null, null
        );

        mockMvc.perform(post("/api/admin/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("startTime is required"));
    }

    @Test
    void deleteScheduleReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/schedules/{scheduleId}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private ScheduleRequest request() {
        return new ScheduleRequest(
                1L, null, ScheduleType.EVENT, "Opening",
                LocalDateTime.of(2026, 8, 1, 9, 0),
                LocalDateTime.of(2026, 8, 1, 10, 0),
                null, ScheduleStatus.SCHEDULED
        );
    }

    private ScheduleResponse response(Long id) {
        ScheduleItem item = ScheduleItem.builder()
                .tournamentId(1L)
                .type(ScheduleType.EVENT)
                .title("Opening")
                .startTime(LocalDateTime.of(2026, 8, 1, 9, 0))
                .build();
        ReflectionTestUtils.setField(item, "id", id);
        return ScheduleResponse.from(item);
    }
}
