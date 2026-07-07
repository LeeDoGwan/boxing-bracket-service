package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.scoring.dto.RoundScoreResponse;
import com.boxing.bracket.scoring.dto.RoundScoreSubmitRequest;
import com.boxing.bracket.scoring.service.JudgeScoreService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/judge")
public class JudgeScoreController {

    private final JudgeScoreService judgeScoreService;

    public JudgeScoreController(@Lazy JudgeScoreService judgeScoreService) {
        this.judgeScoreService = judgeScoreService;
    }

    @PostMapping("/bouts/{boutId}/rounds/{roundNo}/scores")
    public ApiResponse<RoundScoreResponse> submitRoundScore(
            @PathVariable Long boutId,
            @PathVariable Integer roundNo,
            @Valid @RequestBody RoundScoreSubmitRequest request
    ) {
        return ApiResponse.success(judgeScoreService.submitRoundScore(boutId, roundNo, request), "OK");
    }
}
