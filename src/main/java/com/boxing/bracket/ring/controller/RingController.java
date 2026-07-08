package com.boxing.bracket.ring.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.ring.service.RingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rings")
public class RingController {

    private final RingService ringService;

    public RingController(@Lazy RingService ringService) {
        this.ringService = ringService;
    }

    @GetMapping("/status")
    public ApiResponse<List<RingStatusResponse>> getRingStatuses(@RequestParam Long tournamentId) {
        return ApiResponse.success(ringService.getRingStatuses(tournamentId), "OK");
    }
}
