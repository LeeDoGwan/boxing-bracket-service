package com.boxing.bracket.athlete.controller;

import com.boxing.bracket.athlete.dto.AthleteRequest;
import com.boxing.bracket.athlete.dto.AthleteResponse;
import com.boxing.bracket.athlete.service.AdminAthleteService;
import com.boxing.bracket.common.response.ApiResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/admin/athletes")
public class AdminAthleteController {

    private final AdminAthleteService adminAthleteService;

    public AdminAthleteController(@Lazy AdminAthleteService adminAthleteService) {
        this.adminAthleteService = adminAthleteService;
    }

    @PostMapping
    public ApiResponse<AthleteResponse> createAthlete(@Valid @RequestBody AthleteRequest request) {
        return ApiResponse.success(adminAthleteService.createAthlete(request), "OK");
    }

    @PutMapping("/{athleteId}")
    public ApiResponse<AthleteResponse> updateAthlete(
            @PathVariable Long athleteId,
            @Valid @RequestBody AthleteRequest request
    ) {
        return ApiResponse.success(adminAthleteService.updateAthlete(athleteId, request), "OK");
    }

    @DeleteMapping("/{athleteId}")
    public ApiResponse<Void> deleteAthlete(@PathVariable Long athleteId) {
        adminAthleteService.deleteAthlete(athleteId);
        return ApiResponse.success(null, "OK");
    }
}
