package com.boxing.bracket.assignment.controller;

import com.boxing.bracket.assignment.service.StaffAssignmentService;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssignedRingController.class)
class AssignedRingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaffAssignmentService staffAssignmentService;

    @Test
    void getAssignedBoutsUsesStaffAssignmentRoute() throws Exception {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(4L)
                .boutNumber(12)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.IN_PROGRESS)
                .build();
        given(staffAssignmentService.getAssignedBouts(4L))
                .willReturn(List.of(RingManagerBoutResponse.from(bout)));

        mockMvc.perform(get("/api/staff/assignments/rings/{ringId}/bouts", 4L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].boutNumber").value(12))
                .andExpect(jsonPath("$.data[0].status").value("IN_PROGRESS"));
    }
}
