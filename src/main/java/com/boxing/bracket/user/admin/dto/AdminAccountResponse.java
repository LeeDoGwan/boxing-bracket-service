package com.boxing.bracket.user.admin.dto;

import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;

public class AdminAccountResponse {

    private final Long accountId;
    private final String loginId;
    private final String name;
    private final UserRole role;
    private final AccountStatus status;

    private AdminAccountResponse(Long accountId, String loginId, String name, UserRole role, AccountStatus status) {
        this.accountId = accountId;
        this.loginId = loginId;
        this.name = name;
        this.role = role;
        this.status = status;
    }

    public static AdminAccountResponse from(Account account) {
        return new AdminAccountResponse(
                account.getId(),
                account.getLoginId(),
                account.getName(),
                account.getRole(),
                account.getStatus()
        );
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getLoginId() {
        return loginId;
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
