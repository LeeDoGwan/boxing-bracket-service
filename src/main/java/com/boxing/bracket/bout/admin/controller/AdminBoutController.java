package com.boxing.bracket.bout.admin.controller;

import com.boxing.bracket.bout.admin.dto.AdminBoutRequest;
import com.boxing.bracket.bout.admin.dto.AdminBoutResponse;
import com.boxing.bracket.bout.admin.service.AdminBoutService;
import com.boxing.bracket.common.response.ApiResponse;
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
@RequestMapping("/api/admin/bouts")
public class AdminBoutController {

    private final AdminBoutService adminBoutService;

    public AdminBoutController(@Lazy AdminBoutService adminBoutService) {
        this.adminBoutService = adminBoutService;
    }

    @GetMapping
    public ApiResponse<List<AdminBoutResponse>> getBouts(@RequestParam Long tournamentId) {
        return ApiResponse.success(adminBoutService.getBouts(tournamentId), "OK");
    }

    @GetMapping("/{boutId}")
    public ApiResponse<AdminBoutResponse> getBout(@PathVariable Long boutId) {
        return ApiResponse.success(adminBoutService.getBout(boutId), "OK");
    }

    @PostMapping
    public ApiResponse<AdminBoutResponse> createBout(@Valid @RequestBody AdminBoutRequest request) {
        return ApiResponse.success(adminBoutService.createBout(request), "OK");
    }

    @PutMapping("/{boutId}")
    public ApiResponse<AdminBoutResponse> updateBout(
            @PathVariable Long boutId,
            @Valid @RequestBody AdminBoutRequest request
    ) {
        return ApiResponse.success(adminBoutService.updateBout(boutId, request), "OK");
    }

    @DeleteMapping("/{boutId}")
    public ApiResponse<Void> deleteBout(@PathVariable Long boutId) {
        adminBoutService.deleteBout(boutId);
        return ApiResponse.success(null, "OK");
    }
}
