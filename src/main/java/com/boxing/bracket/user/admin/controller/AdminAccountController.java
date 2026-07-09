package com.boxing.bracket.user.admin.controller;

import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.user.admin.dto.AdminAccountRequest;
import com.boxing.bracket.user.admin.dto.AdminAccountResponse;
import com.boxing.bracket.user.admin.service.AdminAccountService;
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
@RequestMapping("/api/admin/accounts")
public class AdminAccountController {

    private final AdminAccountService adminAccountService;

    public AdminAccountController(@Lazy AdminAccountService adminAccountService) {
        this.adminAccountService = adminAccountService;
    }

    @GetMapping
    public ApiResponse<List<AdminAccountResponse>> getAccounts() {
        return ApiResponse.success(adminAccountService.getAccounts(), "OK");
    }

    @GetMapping("/{accountId}")
    public ApiResponse<AdminAccountResponse> getAccount(@PathVariable Long accountId) {
        return ApiResponse.success(adminAccountService.getAccount(accountId), "OK");
    }

    @PostMapping
    public ApiResponse<AdminAccountResponse> createAccount(@Valid @RequestBody AdminAccountRequest request) {
        return ApiResponse.success(adminAccountService.createAccount(request), "OK");
    }

    @PutMapping("/{accountId}")
    public ApiResponse<AdminAccountResponse> updateAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody AdminAccountRequest request
    ) {
        return ApiResponse.success(adminAccountService.updateAccount(accountId, request), "OK");
    }

    @DeleteMapping("/{accountId}")
    public ApiResponse<Void> deleteAccount(@PathVariable Long accountId) {
        adminAccountService.deleteAccount(accountId);
        return ApiResponse.success(null, "OK");
    }
}
