package com.boxing.bracket.operation.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.operation.dto.TournamentOperationStatusResponse;
import com.boxing.bracket.operation.service.TournamentOperationStatusService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/operations")
public class TournamentOperationStatusController {

    private final TournamentOperationStatusService tournamentOperationStatusService;

    public TournamentOperationStatusController(
            @Lazy TournamentOperationStatusService tournamentOperationStatusService
    ) {
        this.tournamentOperationStatusService = tournamentOperationStatusService;
    }

    @GetMapping("/status")
    public ApiResponse<TournamentOperationStatusResponse> getStatus(@RequestParam Long tournamentId) {
        return ApiResponse.success(tournamentOperationStatusService.getStatus(tournamentId), "OK");
    }
}
