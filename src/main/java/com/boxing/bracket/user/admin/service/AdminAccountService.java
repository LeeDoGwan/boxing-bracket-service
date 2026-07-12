package com.boxing.bracket.user.admin.service;

import com.boxing.bracket.user.admin.dto.AdminAccountRequest;
import com.boxing.bracket.user.admin.dto.AdminAccountResponse;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.exception.AccountNotFoundException;
import com.boxing.bracket.user.repository.AccountRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class AdminAccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAccounts() {
        return getAccounts(null, null, null);
    }

    @Transactional(readOnly = true)
    public List<AdminAccountResponse> getAccounts(String keyword, UserRole role, AccountStatus status) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return accountRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(account -> normalizedKeyword.isEmpty()
                        || containsIgnoreCase(account.getLoginId(), normalizedKeyword)
                        || containsIgnoreCase(account.getName(), normalizedKeyword))
                .filter(account -> role == null || role == account.getRole())
                .filter(account -> status == null || status == account.getStatus())
                .map(AdminAccountResponse::from)
                .collect(Collectors.toList());
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    @Transactional(readOnly = true)
    public AdminAccountResponse getAccount(Long accountId) {
        validateAccountId(accountId);
        return accountRepository.findById(accountId)
                .map(AdminAccountResponse::from)
                .orElseThrow(AccountNotFoundException::new);
    }

    public AdminAccountResponse createAccount(AdminAccountRequest request) {
        validateRequest(request);
        validateUniqueLoginId(request.getLoginId(), null);

        Account account = Account.builder()
                .loginId(request.getLoginId())
                .passwordHash(encodePassword(request.getPasswordHash()))
                .name(request.getName())
                .role(request.getRole())
                .status(request.getStatus())
                .build();

        return AdminAccountResponse.from(accountRepository.save(account));
    }

    public AdminAccountResponse updateAccount(Long accountId, AdminAccountRequest request) {
        validateAccountId(accountId);
        validateRequest(request);
        validateUniqueLoginId(request.getLoginId(), accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(AccountNotFoundException::new);
        account.updateInfo(
                request.getLoginId(),
                encodePassword(request.getPasswordHash()),
                request.getName(),
                request.getRole(),
                request.getStatus()
        );

        return AdminAccountResponse.from(accountRepository.save(account));
    }

    public void deleteAccount(Long accountId) {
        validateAccountId(accountId);
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException();
        }

        accountRepository.deleteById(accountId);
    }

    private void validateRequest(AdminAccountRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("account request is required");
        }
        if (request.getLoginId() == null || request.getLoginId().trim().isEmpty()) {
            throw new IllegalArgumentException("loginId is required");
        }
        if (request.getPasswordHash() == null || request.getPasswordHash().trim().isEmpty()) {
            throw new IllegalArgumentException("passwordHash is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        if (request.getRole() == null) {
            throw new IllegalArgumentException("role is required");
        }
    }

    private void validateUniqueLoginId(String loginId, Long currentAccountId) {
        String normalizedLoginId = loginId.trim();
        accountRepository.findByLoginId(normalizedLoginId)
                .filter(account -> currentAccountId == null || !currentAccountId.equals(account.getId()))
                .ifPresent(account -> {
                    throw new IllegalArgumentException("loginId already exists");
                });
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null) {
            throw new IllegalArgumentException("accountId is required");
        }
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password.trim());
    }
}
