package com.boxing.bracket.home.controller;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.home.dto.HomeResponse;
import com.boxing.bracket.home.service.HomeService;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HomeController.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HomeService homeService;

    @Test
    void getHomeReturnsHomeData() throws Exception {
        given(homeService.getHome(1L))
                .willReturn(HomeResponse.of(
                        1L,
                        List.of(RingStatusResponse.of(createRing(1L), null, null)),
                        List.of(BoutListResponse.of(
                                createConfirmedBout(10L),
                                createAthlete(10L, "Hong Gil Dong"),
                                createAthlete(11L, "Kim Chul Soo")
                        ))
                ));

        mockMvc.perform(get("/api/home")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tournamentId").value(1))
                .andExpect(jsonPath("$.data.ringStatuses[0].ringId").value(1))
                .andExpect(jsonPath("$.data.confirmedResults[0].boutId").value(10));
    }

    @Test
    void getHomeReturnsBadRequestWithoutTournamentId() throws Exception {
        mockMvc.perform(get("/api/home"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    private Ring createRing(Long id) {
        Ring ring = Ring.builder()
                .tournamentId(1L)
                .name("Ring " + id)
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }

    private Bout createConfirmedBout(Long id) {
        Bout bout = Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .matchType("75 - middle school")
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(BoutStatus.FINISHED)
                .scheduledOrder(1)
                .build();
        ReflectionTestUtils.setField(bout, "id", id);
        bout.confirmResult(BoutSide.RED);
        return bout;
    }

    private Athlete createAthlete(Long id, String name) {
        Athlete athlete = Athlete.builder()
                .name(name)
                .affiliation("Club " + id)
                .build();
        ReflectionTestUtils.setField(athlete, "id", id);
        return athlete;
    }
}
