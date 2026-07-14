package com.boxing.bracket.notice.controller;

import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.service.NoticeService;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
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

@WebMvcTest(NoticeController.class)
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoticeService noticeService;

    @Test
    void getActiveNoticesReturnsNoticeList() throws Exception {
        given(noticeService.getActiveNotices(1L)).willReturn(List.of(response(10L)));

        mockMvc.perform(get("/api/notices")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].noticeId").value(10))
                .andExpect(jsonPath("$.data[0].title").value("Notice 10"));
    }

    @Test
    void getActiveNoticesReturnsBadRequestWithoutTournamentId() throws Exception {
        mockMvc.perform(get("/api/notices"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("tournamentId is required"));
    }

    @Test
    void getActiveNoticesReturnsNotFoundForMissingTournament() throws Exception {
        given(noticeService.getActiveNotices(99L)).willThrow(new TournamentNotFoundException());

        mockMvc.perform(get("/api/notices")
                        .param("tournamentId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    private NoticeResponse response(Long id) {
        Notice notice = Notice.builder()
                .tournamentId(1L)
                .title("Notice " + id)
                .content("Content " + id)
                .active(true)
                .displayOrder(1)
                .build();
        ReflectionTestUtils.setField(notice, "id", id);
        return NoticeResponse.from(notice);
    }
}
