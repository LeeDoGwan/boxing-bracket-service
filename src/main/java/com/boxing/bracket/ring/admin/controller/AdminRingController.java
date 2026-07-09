package com.boxing.bracket.ring.admin.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.ring.admin.dto.AdminRingRequest;
import com.boxing.bracket.ring.admin.dto.AdminRingResponse;
import com.boxing.bracket.ring.admin.service.AdminRingService;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/admin/rings")
public class AdminRingController {

    private final AdminRingService adminRingService;

    public AdminRingController(@Lazy AdminRingService adminRingService) {
        this.adminRingService = adminRingService;
    }

    @GetMapping
    public ApiResponse<List<AdminRingResponse>> getRings(@RequestParam Long tournamentId) {
        return ApiResponse.success(adminRingService.getRings(tournamentId), "OK");
    }

    @GetMapping("/{ringId}")
    public ApiResponse<AdminRingResponse> getRing(@PathVariable Long ringId) {
        return ApiResponse.success(adminRingService.getRing(ringId), "OK");
    }

    @PostMapping
    public ApiResponse<AdminRingResponse> createRing(@Valid @RequestBody AdminRingRequest request) {
        return ApiResponse.success(adminRingService.createRing(request), "OK");
    }

    @PutMapping("/{ringId}")
    public ApiResponse<AdminRingResponse> updateRing(
            @PathVariable Long ringId,
            @Valid @RequestBody AdminRingRequest request
    ) {
        return ApiResponse.success(adminRingService.updateRing(ringId, request), "OK");
    }

    @DeleteMapping("/{ringId}")
    public ApiResponse<Void> deleteRing(@PathVariable Long ringId) {
        adminRingService.deleteRing(ringId);
        return ApiResponse.success(null, "OK");
    }
}
