package com.boxing.bracket.user.repository;

import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void findsAccountByLoginId() {
        accountRepository.saveAndFlush(Account.builder()
                .loginId("judge01")
                .passwordHash("hash")
                .name("Judge 01")
                .role(UserRole.JUDGE)
                .status(AccountStatus.ACTIVE)
                .build());

        Account found = accountRepository.findByLoginId("judge01").orElseThrow();

        assertThat(accountRepository.existsByLoginId("judge01")).isTrue();
        assertThat(found.getRole()).isEqualTo(UserRole.JUDGE);
        assertThat(found.getStatus()).isEqualTo(AccountStatus.ACTIVE);
    }
}
