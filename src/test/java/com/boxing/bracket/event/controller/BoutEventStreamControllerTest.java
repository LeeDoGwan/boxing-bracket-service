package com.boxing.bracket.event.controller;

import com.boxing.bracket.event.service.BoutEventStreamService;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoutEventStreamController.class)
class BoutEventStreamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoutEventStreamService boutEventStreamService;

    @Test
    void streamStartsAsyncSseRequest() throws Exception {
        given(boutEventStreamService.subscribe(1L, 2L)).willReturn(new SseEmitter());

        mockMvc.perform(get("/api/events/stream")
                        .param("tournamentId", "1")
                        .param("ringId", "2"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());
    }

    @Test
    void streamReturnsBadRequestWithoutTournamentId() throws Exception {
        mockMvc.perform(get("/api/events/stream"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void streamReturnsNotFoundForMissingTournament() throws Exception {
        given(boutEventStreamService.subscribe(99L, null)).willThrow(new TournamentNotFoundException());

        mockMvc.perform(get("/api/events/stream")
                        .param("tournamentId", "99"))
                .andExpect(status().isNotFound());
    }
}
