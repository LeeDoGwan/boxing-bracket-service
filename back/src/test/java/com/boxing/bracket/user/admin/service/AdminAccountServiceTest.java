package com.boxing.bracket.user.admin.service;

import com.boxing.bracket.user.admin.dto.AdminAccountRequest;
import com.boxing.bracket.user.admin.dto.AdminAccountResponse;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.exception.AccountNotFoundException;
import com.boxing.bracket.user.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Mock
    private AccountRepository accountRepository;

    private AdminAccountService adminAccountService;

    @BeforeEach
    void setUp() {
        adminAccountService = new AdminAccountService(accountRepository, passwordEncoder);
    }

    @Test
    void getAccountsReturnsAccountList() {
        given(accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .willReturn(List.of(createAccount(1L), createAccount(2L)));

        List<AdminAccountResponse> responses = adminAccountService.getAccounts();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getAccountId()).isEqualTo(1L);
        assertThat(responses.get(0).getLoginId()).isEqualTo("judge01");
    }

    @Test
    void getAccountsFiltersByKeywordRoleAndStatus() {
        given(accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
                .willReturn(List.of(
                        createAccount(1L),
                        createAccount(2L, "ring01", "Ring One", UserRole.RING_MANAGER, AccountStatus.INACTIVE)
                ));

        List<AdminAccountResponse> responses = adminAccountService.getAccounts(
                " ring ", UserRole.RING_MANAGER, AccountStatus.INACTIVE
        );

        assertThat(responses).extracting(AdminAccountResponse::getLoginId).containsExactly("ring01");
    }

    @Test
    void getAccountReturnsAccount() {
        given(accountRepository.findById(1L)).willReturn(Optional.of(createAccount(1L)));

        AdminAccountResponse response = adminAccountService.getAccount(1L);

        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Judge One");
    }

    @Test
    void getAccountRejectsMissingAccount() {
        given(accountRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminAccountService.getAccount(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    @Test
    void createAccountSavesAccount() {
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.empty());
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            ReflectionTestUtils.setField(account, "id", 1L);
            return account;
        });

        AdminAccountResponse response = adminAccountService.createAccount(request());

        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getLoginId()).isEqualTo("judge01");
        assertThat(response.getRole()).isEqualTo(UserRole.JUDGE);
        then(accountRepository).should().save(argThat(account ->
                !account.getPasswordHash().equals("hash1")
                        && passwordEncoder.matches("hash1", account.getPasswordHash())
        ));
    }

    @Test
    void createAccountRejectsDuplicateLoginId() {
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(createAccount(1L)));

        assertThatThrownBy(() -> adminAccountService.createAccount(request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("loginId already exists");
    }

    @Test
    void createAccountRejectsMissingPasswordHash() {
        AdminAccountRequest request = new AdminAccountRequest("judge01", " ", "Judge One", UserRole.JUDGE, null);

        assertThatThrownBy(() -> adminAccountService.createAccount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("passwordHash is required");
    }

    @Test
    void updateAccountChangesAccount() {
        Account account = createAccount(1L);
        AdminAccountRequest request = new AdminAccountRequest(
                "judge02",
                "hash2",
                "Judge Two",
                UserRole.SUPERVISOR,
                AccountStatus.INACTIVE
        );
        given(accountRepository.findByLoginId("judge02")).willReturn(Optional.empty());
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminAccountResponse response = adminAccountService.updateAccount(1L, request);

        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getLoginId()).isEqualTo("judge02");
        assertThat(response.getRole()).isEqualTo(UserRole.SUPERVISOR);
        assertThat(response.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(account.getPasswordHash()).isNotEqualTo("hash2");
        assertThat(passwordEncoder.matches("hash2", account.getPasswordHash())).isTrue();
    }

    @Test
    void updateAccountAllowsSameLoginId() {
        Account account = createAccount(1L);
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.of(account));
        given(accountRepository.findById(1L)).willReturn(Optional.of(account));
        given(accountRepository.save(any(Account.class))).willAnswer(invocation -> invocation.getArgument(0));

        AdminAccountResponse response = adminAccountService.updateAccount(1L, request());

        assertThat(response.getAccountId()).isEqualTo(1L);
        assertThat(response.getLoginId()).isEqualTo("judge01");
    }

    @Test
    void updateAccountRejectsMissingAccount() {
        given(accountRepository.findByLoginId("judge01")).willReturn(Optional.empty());
        given(accountRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> adminAccountService.updateAccount(99L, request()))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    @Test
    void deleteAccountDeletesExistingAccount() {
        given(accountRepository.existsById(1L)).willReturn(true);

        adminAccountService.deleteAccount(1L);

        then(accountRepository).should().deleteById(1L);
    }

    @Test
    void deleteAccountRejectsMissingAccount() {
        given(accountRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> adminAccountService.deleteAccount(99L))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    private AdminAccountRequest request() {
        return new AdminAccountRequest(" judge01 ", " hash1 ", " Judge One ", UserRole.JUDGE, null);
    }

    private Account createAccount(Long id) {
        return createAccount(id, "judge01", "Judge One", UserRole.JUDGE, AccountStatus.ACTIVE);
    }

    private Account createAccount(Long id, String loginId, String name, UserRole role, AccountStatus status) {
        Account account = Account.builder()
                .loginId(loginId)
                .passwordHash("hash1")
                .name(name)
                .role(role)
                .status(status)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
