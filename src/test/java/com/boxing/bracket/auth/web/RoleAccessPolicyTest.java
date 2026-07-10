package com.boxing.bracket.auth.web;

import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAccessPolicyTest {

    private final RoleAccessPolicy roleAccessPolicy = new RoleAccessPolicy();

    @Test
    void authMeRequiresAuthenticatedSession() {
        Optional<AccessRule> rule = roleAccessPolicy.findRule("/api/auth/me");

        assertThat(rule).isPresent();
        assertThat(rule.get().hasRoleRestriction()).isFalse();
    }

    @Test
    void adminAccountRequiresServiceManager() {
        Optional<AccessRule> rule = roleAccessPolicy.findRule("/api/admin/accounts");

        assertThat(rule).isPresent();
        assertThat(rule.get().getRoles()).containsExactly(UserRole.SERVICE_MANAGER);
    }

    @Test
    void adminTournamentAllowsGameAndServiceManager() {
        Optional<AccessRule> rule = roleAccessPolicy.findRule("/api/admin/tournaments");

        assertThat(rule).isPresent();
        assertThat(rule.get().getRoles()).containsExactly(UserRole.GAME_MANAGER, UserRole.SERVICE_MANAGER);
    }

    @Test
    void judgeApiRequiresJudge() {
        Optional<AccessRule> rule = roleAccessPolicy.findRule("/api/judge/bouts/1/scores");

        assertThat(rule).isPresent();
        assertThat(rule.get().getRoles()).containsExactly(UserRole.JUDGE);
    }

    @Test
    void publicAudienceApiHasNoRule() {
        Optional<AccessRule> rule = roleAccessPolicy.findRule("/api/home");

        assertThat(rule).isEmpty();
    }
}
