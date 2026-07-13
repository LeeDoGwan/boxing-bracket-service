package com.boxing.bracket.schedule.controller;

import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.boxing.bracket.schedule.domain.ScheduleItem;
import com.boxing.bracket.schedule.domain.ScheduleType;
import com.boxing.bracket.schedule.service.ScheduleService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ScheduleController.class)
class ScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ScheduleService scheduleService;

    @Test
    void getSchedulesReturnsPublicScheduleList() throws Exception {
        given(scheduleService.getSchedules(1L)).willReturn(List.of(response(7L)));

        mockMvc.perform(get("/api/schedules").param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].scheduleId").value(7))
                .andExpect(jsonPath("$.data[0].type").value("EVENT"));
    }

    @Test
    void getSchedulesRequiresTournamentId() throws Exception {
        mockMvc.perform(get("/api/schedules"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
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
