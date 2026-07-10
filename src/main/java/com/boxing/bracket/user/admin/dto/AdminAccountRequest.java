package com.boxing.bracket.user.admin.dto;

import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AdminAccountRequest {

    @NotBlank(message = "loginId is required")
    private String loginId;

    @NotBlank(message = "passwordHash is required")
    private String passwordHash;

    @NotBlank(message = "name is required")
    private String name;

    @NotNull(message = "role is required")
    private UserRole role;

    private AccountStatus status;

    protected AdminAccountRequest() {
    }

    public AdminAccountRequest(
            String loginId,
            String passwordHash,
            String name,
            UserRole role,
            AccountStatus status
    ) {
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public String getLoginId() {
        return loginId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
