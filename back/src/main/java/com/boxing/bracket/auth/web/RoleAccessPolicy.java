package com.boxing.bracket.auth.web;

import com.boxing.bracket.user.domain.UserRole;

import java.util.Optional;

public class RoleAccessPolicy {

    public Optional<AccessRule> findRule(String requestUri) {
        if (requestUri == null) {
            return Optional.empty();
        }
        if (requestUri.startsWith("/api/auth/logout") || requestUri.startsWith("/api/auth/me")) {
            return Optional.of(AccessRule.authenticated());
        }
        if (requestUri.startsWith("/api/admin/accounts")) {
            return Optional.of(AccessRule.roles(UserRole.SERVICE_MANAGER));
        }
        if (requestUri.startsWith("/api/admin")) {
            return Optional.of(AccessRule.roles(UserRole.GAME_MANAGER, UserRole.SERVICE_MANAGER));
        }
        if (requestUri.startsWith("/api/judge")) {
            return Optional.of(AccessRule.roles(UserRole.JUDGE));
        }
        if (requestUri.startsWith("/api/supervisor")) {
            return Optional.of(AccessRule.roles(UserRole.SUPERVISOR));
        }
        if (requestUri.startsWith("/api/ring-manager")) {
            return Optional.of(AccessRule.roles(UserRole.RING_MANAGER));
        }
        if (requestUri.startsWith("/api/staff")) {
            return Optional.of(AccessRule.roles(UserRole.JUDGE, UserRole.SUPERVISOR, UserRole.RING_MANAGER));
        }
        return Optional.empty();
    }
}
