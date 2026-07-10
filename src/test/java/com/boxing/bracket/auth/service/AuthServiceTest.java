package com.boxing.bracket.auth.service;

import com.boxing.bracket.auth.dto.AuthAccountResponse;
import com.boxing.bracket.auth.dto.LoginRequest;
import com.boxing.bracket.auth.dto.LoginResponse;
import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.auth.exception.AuthenticationRequiredException;
import com.boxing.bracket.auth.exception.InvalidCredentialsException;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Test
    void loginCreatesSessionForActiveAccount() {
        AuthService authService = authService();
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account(UserRole.JUDGE)));

        LoginResponse response = authService.login(new LoginRequest(" judge01 ", "password1"));

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getAccount().getLoginId()).isEqualTo("judge01");
        assertThat(response.getAccount().getRole()).isEqualTo(UserRole.JUDGE);
    }

    @Test
    void loginRejectsWrongPassword() {
        AuthService authService = authService();
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account(UserRole.JUDGE)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("judge01", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void loginRejectsInactiveAccount() {
        AuthService authService = authService();
        Account account = account(UserRole.JUDGE);
        account.updateInfo("judge01", "password1", "Judge One", UserRole.JUDGE, AccountStatus.INACTIVE);
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account));

        assertThatThrownBy(() -> authService.login(new LoginRequest("judge01", "password1")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void meReturnsCurrentAccount() {
        AuthService authService = authService();
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account(UserRole.JUDGE)));
        LoginResponse loginResponse = authService.login(new LoginRequest("judge01", "password1"));

        AuthAccountResponse response = authService.me("Bearer " + loginResponse.getAccessToken());

        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Judge One");
    }

    @Test
    void logoutExpiresSession() {
        AuthService authService = authService();
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account(UserRole.JUDGE)));
        LoginResponse loginResponse = authService.login(new LoginRequest("judge01", "password1"));

        authService.logout("Bearer " + loginResponse.getAccessToken());

        assertThatThrownBy(() -> authService.me("Bearer " + loginResponse.getAccessToken()))
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessage("Authentication required");
    }

    @Test
    void requireRoleRejectsDifferentRole() {
        AuthService authService = authService();
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account(UserRole.JUDGE)));
        LoginResponse loginResponse = authService.login(new LoginRequest("judge01", "password1"));

        assertThatThrownBy(() -> authService.requireRole(
                "Bearer " + loginResponse.getAccessToken(),
                UserRole.SUPERVISOR
        ))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Access denied");
    }

    @Test
    void requireRejectsMissingBearerHeader() {
        AuthService authService = authService();

        assertThatThrownBy(() -> authService.require("token-only"))
                .isInstanceOf(AuthenticationRequiredException.class)
                .hasMessage("Authentication required");
    }

    private AuthService authService() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-10T00:00:00Z"), ZoneId.of("UTC"));
        return new AuthService(accountRepository, clock);
    }

    private Account account(UserRole role) {
        Account account = Account.builder()
                .loginId("judge01")
                .passwordHash("password1")
                .name("Judge One")
                .role(role)
                .status(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }
}
