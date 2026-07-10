package com.boxing.bracket.notice.admin.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.notice.admin.service.AdminNoticeService;
import com.boxing.bracket.notice.dto.NoticeRequest;
import com.boxing.bracket.notice.dto.NoticeResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {

    private final AdminNoticeService adminNoticeService;

    public AdminNoticeController(@Lazy AdminNoticeService adminNoticeService) {
        this.adminNoticeService = adminNoticeService;
    }

    @GetMapping
    public ApiResponse<List<NoticeResponse>> getNotices(@RequestParam Long tournamentId) {
        return ApiResponse.success(adminNoticeService.getNotices(tournamentId), "OK");
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeResponse> getNotice(@PathVariable Long noticeId) {
        return ApiResponse.success(adminNoticeService.getNotice(noticeId), "OK");
    }

    @PostMapping
    public ApiResponse<NoticeResponse> createNotice(@Valid @RequestBody NoticeRequest request) {
        return ApiResponse.success(adminNoticeService.createNotice(request), "OK");
    }

    @PutMapping("/{noticeId}")
    public ApiResponse<NoticeResponse> updateNotice(
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeRequest request
    ) {
        return ApiResponse.success(adminNoticeService.updateNotice(noticeId, request), "OK");
    }

    @DeleteMapping("/{noticeId}")
    public ApiResponse<Void> deleteNotice(@PathVariable Long noticeId) {
        adminNoticeService.deleteNotice(noticeId);
        return ApiResponse.success(null, "OK");
    }
}
