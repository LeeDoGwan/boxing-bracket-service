package com.boxing.bracket.user.admin.service;

import com.boxing.bracket.user.admin.dto.AdminAccountRequest;
import com.boxing.bracket.user.admin.dto.AdminAccountResponse;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.exception.AccountNotFoundException;
import com.boxing.bracket.user.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AdminAccountService adminAccountService;

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
        Account account = Account.builder()
                .loginId("judge01")
                .passwordHash("hash1")
                .name("Judge One")
                .role(UserRole.JUDGE)
                .status(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
