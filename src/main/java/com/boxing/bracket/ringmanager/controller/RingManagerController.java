package com.boxing.bracket.ringmanager.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.ringmanager.dto.BoutStatusUpdateRequest;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import com.boxing.bracket.ringmanager.service.RingManagerService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

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

    @GetMapping("/rings/{ringId}/bouts")
    public ApiResponse<List<RingManagerBoutResponse>> getRingBouts(@PathVariable Long ringId) {
        return ApiResponse.success(ringManagerService.getRingBouts(ringId), "OK");
    }

    @PostMapping("/bouts/{boutId}/status")
    public ApiResponse<RingManagerBoutResponse> updateBoutStatus(
            @PathVariable Long boutId,
            @Valid @RequestBody BoutStatusUpdateRequest request
    ) {
        return ApiResponse.success(ringManagerService.updateBoutStatus(boutId, request), "OK");
    }

    @PostMapping("/rings/{ringId}/next")
    public ApiResponse<RingManagerBoutResponse> moveToNextBout(@PathVariable Long ringId) {
        return ApiResponse.success(ringManagerService.moveToNextBout(ringId), "OK");
    }
}
