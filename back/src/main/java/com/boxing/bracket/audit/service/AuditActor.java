package com.boxing.bracket.audit.service;

import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.user.domain.UserRole;

public class AuditActor {

    private final Long accountId;
    private final String username;
    private final UserRole role;

    public AuditActor(Long accountId, String username, UserRole role) {
        this.accountId = accountId;
        this.username = username;
        this.role = role;
    }

    public static AuditActor from(AuthSession session) {
        return new AuditActor(session.getAccountId(), session.getLoginId(), session.getRole());
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public UserRole getRole() {
        return role;
    }
}
