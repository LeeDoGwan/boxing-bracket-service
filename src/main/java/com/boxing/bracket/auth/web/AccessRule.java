package com.boxing.bracket.auth.web;

import com.boxing.bracket.user.domain.UserRole;

public class AccessRule {

    private final UserRole[] roles;

    private AccessRule(UserRole[] roles) {
        this.roles = roles;
    }

    public static AccessRule authenticated() {
        return new AccessRule(new UserRole[0]);
    }

    public static AccessRule roles(UserRole... roles) {
        return new AccessRule(roles);
    }

    public boolean hasRoleRestriction() {
        return roles.length > 0;
    }

    public UserRole[] getRoles() {
        return roles;
    }
}
