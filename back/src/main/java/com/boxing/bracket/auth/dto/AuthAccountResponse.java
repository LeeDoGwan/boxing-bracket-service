package com.boxing.bracket.auth.dto;

import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.UserRole;

public class AuthAccountResponse {

    private final Long accountId;
    private final String loginId;
    private final String name;
    private final UserRole role;

    private AuthAccountResponse(Long accountId, String loginId, String name, UserRole role) {
        this.accountId = accountId;
        this.loginId = loginId;
        this.name = name;
        this.role = role;
    }

    public static AuthAccountResponse from(Account account) {
        return new AuthAccountResponse(
                account.getId(),
                account.getLoginId(),
                account.getName(),
                account.getRole()
        );
    }

    public static AuthAccountResponse from(AuthSession session) {
        return new AuthAccountResponse(
                session.getAccountId(),
                session.getLoginId(),
                session.getName(),
                session.getRole()
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
}
