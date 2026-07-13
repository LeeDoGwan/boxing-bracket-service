package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.scoring.dto.PenaltyCreateRequest;
import com.boxing.bracket.scoring.dto.PenaltyResponse;
import com.boxing.bracket.scoring.service.SupervisorPenaltyService;
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
@RequestMapping("/api/supervisor")
public class SupervisorPenaltyController {

    private final SupervisorPenaltyService supervisorPenaltyService;

    public SupervisorPenaltyController(@Lazy SupervisorPenaltyService supervisorPenaltyService) {
        this.supervisorPenaltyService = supervisorPenaltyService;
    }

    @GetMapping("/bouts/{boutId}/penalties")
    public ApiResponse<List<PenaltyResponse>> getPenalties(@PathVariable Long boutId) {
        return ApiResponse.success(supervisorPenaltyService.getPenalties(boutId), "OK");
    }

    @PostMapping("/bouts/{boutId}/penalties")
    public ApiResponse<PenaltyResponse> createPenalty(
            @PathVariable Long boutId,
            @Valid @RequestBody PenaltyCreateRequest request
    ) {
        return ApiResponse.success(supervisorPenaltyService.createPenalty(boutId, request), "OK");
    }
}
