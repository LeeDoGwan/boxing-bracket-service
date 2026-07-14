package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.service.JudgeScoreService;
import com.boxing.bracket.scoring.service.ScoreQueryService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/judge")
public class JudgeScoreController {

    private final JudgeScoreService judgeScoreService;
    private final ScoreQueryService scoreQueryService;

    public JudgeScoreController(@Lazy JudgeScoreService judgeScoreService, @Lazy ScoreQueryService scoreQueryService) {
        this.judgeScoreService = judgeScoreService;
        this.scoreQueryService = scoreQueryService;
    }

    @PostMapping("/bouts/{boutId}/rounds/{roundNo}/scores")
    public ApiResponse<RoundScoreResponse> submitRoundScore(
            @PathVariable Long boutId,
            @PathVariable Integer roundNo,
            @Valid @RequestBody RoundScoreSubmitRequest request
    ) {
        return ApiResponse.success(judgeScoreService.submitRoundScore(boutId, roundNo, request), "OK");
    }

    @GetMapping("/bouts/{boutId}/scores")
    public ApiResponse<List<RoundScoreResponse>> getBoutScores(
            @PathVariable Long boutId,
            @RequestParam(required = false) Long judgeId
    ) {
        return ApiResponse.success(scoreQueryService.getBoutScores(boutId, judgeId), "OK");
    }
}
