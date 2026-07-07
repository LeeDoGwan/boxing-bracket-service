package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.service.ScoreQueryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/supervisor")
public class SupervisorScoreController {

    private final ScoreQueryService scoreQueryService;

    public SupervisorScoreController(@Lazy ScoreQueryService scoreQueryService) {
        this.scoreQueryService = scoreQueryService;
    }

    @GetMapping("/bouts/{boutId}/scores")
    public ApiResponse<List<RoundScoreResponse>> getBoutScores(@PathVariable Long boutId) {
        return ApiResponse.success(scoreQueryService.getBoutScores(boutId), "OK");
    }
}
