package com.boxing.bracket.user.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    @Test
    void updateInfoChangesAccountFields() {
        Account account = createAccount();

        account.updateInfo(" judge02 ", " hash2 ", " Judge Two ", UserRole.SUPERVISOR, AccountStatus.INACTIVE);

        assertThat(account.getLoginId()).isEqualTo("judge02");
        assertThat(account.getPasswordHash()).isEqualTo("hash2");
        assertThat(account.getName()).isEqualTo("Judge Two");
        assertThat(account.getRole()).isEqualTo(UserRole.SUPERVISOR);
        assertThat(account.getStatus()).isEqualTo(AccountStatus.INACTIVE);
    }

    @Test
    void updateInfoRejectsMissingRole() {
        Account account = createAccount();

        assertThatThrownBy(() -> account.updateInfo("judge02", "hash2", "Judge Two", null, AccountStatus.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("role is required");
    }

    private Account createAccount() {
        return Account.builder()
                .loginId("judge01")
                .passwordHash("hash1")
                .name("Judge One")
                .role(UserRole.JUDGE)
                .build();
    }
}
