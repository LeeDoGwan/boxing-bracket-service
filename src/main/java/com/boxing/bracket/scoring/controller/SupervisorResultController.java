package com.boxing.bracket.scoring.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.scoring.dto.BoutResultConfirmRequest;
import com.boxing.bracket.scoring.dto.BoutResultResponse;
import com.boxing.bracket.scoring.service.SupervisorResultService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/supervisor")
public class SupervisorResultController {

    private final SupervisorResultService supervisorResultService;

    public SupervisorResultController(@Lazy SupervisorResultService supervisorResultService) {
        this.supervisorResultService = supervisorResultService;
    }

    @PostMapping("/bouts/{boutId}/result")
    public ApiResponse<BoutResultResponse> confirmResult(
            @PathVariable Long boutId,
            @Valid @RequestBody BoutResultConfirmRequest request
    ) {
        return ApiResponse.success(supervisorResultService.confirmResult(boutId, request), "OK");
    }
}
