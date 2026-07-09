package com.boxing.bracket.tournament.admin.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.tournament.admin.dto.AdminTournamentRequest;
import com.boxing.bracket.tournament.admin.dto.AdminTournamentResponse;
import com.boxing.bracket.tournament.admin.service.AdminTournamentService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tournaments")
public class AdminTournamentController {

    private final AdminTournamentService adminTournamentService;

    public AdminTournamentController(@Lazy AdminTournamentService adminTournamentService) {
        this.adminTournamentService = adminTournamentService;
    }

    @GetMapping
    public ApiResponse<List<AdminTournamentResponse>> getTournaments() {
        return ApiResponse.success(adminTournamentService.getTournaments(), "OK");
    }

    @GetMapping("/{tournamentId}")
    public ApiResponse<AdminTournamentResponse> getTournament(@PathVariable Long tournamentId) {
        return ApiResponse.success(adminTournamentService.getTournament(tournamentId), "OK");
    }

    @PostMapping
    public ApiResponse<AdminTournamentResponse> createTournament(
            @Valid @RequestBody AdminTournamentRequest request
    ) {
        return ApiResponse.success(adminTournamentService.createTournament(request), "OK");
    }

    @PutMapping("/{tournamentId}")
    public ApiResponse<AdminTournamentResponse> updateTournament(
            @PathVariable Long tournamentId,
            @Valid @RequestBody AdminTournamentRequest request
    ) {
        return ApiResponse.success(adminTournamentService.updateTournament(tournamentId, request), "OK");
    }

    @DeleteMapping("/{tournamentId}")
    public ApiResponse<Void> deleteTournament(@PathVariable Long tournamentId) {
        adminTournamentService.deleteTournament(tournamentId);
        return ApiResponse.success(null, "OK");
    }
}
