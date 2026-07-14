package com.boxing.bracket.notice.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.notice.dto.NoticeResponse;
import com.boxing.bracket.notice.service.NoticeService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(@Lazy NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public ApiResponse<List<NoticeResponse>> getActiveNotices(@RequestParam Long tournamentId) {
        return ApiResponse.success(noticeService.getActiveNotices(tournamentId), "OK");
    }
}
