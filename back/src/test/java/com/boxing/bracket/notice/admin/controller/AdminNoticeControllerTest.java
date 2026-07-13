package com.boxing.bracket.notice.admin.controller;

import com.boxing.bracket.notice.admin.service.AdminNoticeService;
import com.boxing.bracket.notice.domain.Notice;
import com.boxing.bracket.notice.dto.NoticeRequest;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.exception.NoticeNotFoundException;
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

@WebMvcTest(AdminNoticeController.class)
class AdminNoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminNoticeService adminNoticeService;

    @Test
    void getNoticesReturnsNoticeList() throws Exception {
        given(adminNoticeService.getNotices(1L)).willReturn(List.of(response(10L)));

        mockMvc.perform(get("/api/admin/notices")
                        .param("tournamentId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].noticeId").value(10));
    }

    @Test
    void getNoticeReturnsNotice() throws Exception {
        given(adminNoticeService.getNotice(10L)).willReturn(response(10L));

        mockMvc.perform(get("/api/admin/notices/{noticeId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noticeId").value(10));
    }

    @Test
    void getNoticeReturnsNotFoundForMissingNotice() throws Exception {
        given(adminNoticeService.getNotice(99L)).willThrow(new NoticeNotFoundException());

        mockMvc.perform(get("/api/admin/notices/{noticeId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notice not found"));
    }

    @Test
    void createNoticeReturnsCreatedNotice() throws Exception {
        given(adminNoticeService.createNotice(any(NoticeRequest.class))).willReturn(response(10L));

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noticeId").value(10))
                .andExpect(jsonPath("$.data.title").value("Notice 10"));
    }

    @Test
    void createNoticeReturnsBadRequestForBlankTitle() throws Exception {
        NoticeRequest request = new NoticeRequest(1L, " ", "Content", true, 1);

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    @Test
    void createNoticeReturnsNotFoundForMissingTournament() throws Exception {
        given(adminNoticeService.createNotice(any(NoticeRequest.class)))
                .willThrow(new TournamentNotFoundException());

        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NoticeRequest(99L, "Notice", "Content", true, 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Tournament not found"));
    }

    @Test
    void updateNoticeReturnsUpdatedNotice() throws Exception {
        given(adminNoticeService.updateNotice(eq(10L), any(NoticeRequest.class))).willReturn(response(10L));

        mockMvc.perform(put("/api/admin/notices/{noticeId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.noticeId").value(10));
    }

    @Test
    void updateNoticeReturnsNotFoundForMissingNotice() throws Exception {
        given(adminNoticeService.updateNotice(eq(99L), any(NoticeRequest.class)))
                .willThrow(new NoticeNotFoundException());

        mockMvc.perform(put("/api/admin/notices/{noticeId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notice not found"));
    }

    @Test
    void deleteNoticeReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/notices/{noticeId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void deleteNoticeReturnsNotFoundForMissingNotice() throws Exception {
        willThrow(new NoticeNotFoundException()).given(adminNoticeService).deleteNotice(99L);

        mockMvc.perform(delete("/api/admin/notices/{noticeId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Notice not found"));
    }

    private NoticeRequest request() {
        return new NoticeRequest(1L, "Notice", "Content", true, 1);
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
