package com.boxing.bracket.ringmanager.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import com.boxing.bracket.ringmanager.service.RingManagerService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ring-manager")
public class RingManagerController {

    private final RingManagerService ringManagerService;

    public RingManagerController(@Lazy RingManagerService ringManagerService) {
        this.ringManagerService = ringManagerService;
    }

    @PostMapping("/bouts/{boutId}/start")
    public ApiResponse<RingManagerBoutResponse> startBout(@PathVariable Long boutId) {
        return ApiResponse.success(ringManagerService.startBout(boutId), "OK");
    }
}
