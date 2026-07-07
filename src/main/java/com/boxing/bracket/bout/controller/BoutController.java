package com.boxing.bracket.bout.controller;

import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.service.BoutService;
import com.boxing.bracket.common.response.ApiResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bouts")
public class BoutController {

    private final BoutService boutService;

    public BoutController(@Lazy BoutService boutService) {
        this.boutService = boutService;
    }

    @GetMapping
    public ApiResponse<List<BoutListResponse>> getOfficialBouts(@RequestParam Long tournamentId) {
        return ApiResponse.success(boutService.getOfficialBouts(tournamentId), "OK");
    }

    @GetMapping("/{boutId}")
    public ApiResponse<BoutDetailResponse> getBoutDetail(@PathVariable Long boutId) {
        return ApiResponse.success(boutService.getBoutDetail(boutId), "OK");
    }
}
