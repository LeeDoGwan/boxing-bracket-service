package com.boxing.bracket.auth.domain;

import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.UserRole;

import java.time.LocalDateTime;

public class AuthSession {

    private final String token;
    private final Long accountId;
    private final String loginId;
    private final String name;
    private final UserRole role;
    private final LocalDateTime issuedAt;
    private final LocalDateTime expiresAt;
    private boolean active = true;

    public AuthSession(String token, Account account, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("token is required");
        }
        if (account == null) {
            throw new IllegalArgumentException("account is required");
        }
        if (issuedAt == null) {
            throw new IllegalArgumentException("issuedAt is required");
        }
        if (expiresAt == null) {
            throw new IllegalArgumentException("expiresAt is required");
        }
        if (!expiresAt.isAfter(issuedAt)) {
            throw new IllegalArgumentException("expiresAt must be after issuedAt");
        }

        this.token = token;
        this.accountId = account.getId();
        this.loginId = account.getLoginId();
        this.name = account.getName();
        this.role = account.getRole();
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public boolean isAvailable(LocalDateTime now) {
        return active && now != null && now.isBefore(expiresAt);
    }

    public void expire() {
        this.active = false;
    }

    public String getToken() {
        return token;
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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isActive() {
        return active;
    }
}
