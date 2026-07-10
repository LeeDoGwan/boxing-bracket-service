package com.boxing.bracket.auth.service;

import com.boxing.bracket.auth.domain.AuthSession;
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
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Lazy
@Transactional(readOnly = true)
public class AuthService {

    private static final long SESSION_HOURS = 12L;

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final Map<String, AuthSession> sessions = new ConcurrentHashMap<>();

    public AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this(accountRepository, passwordEncoder, Clock.systemDefaultZone());
    }

    AuthService(AccountRepository accountRepository, PasswordEncoder passwordEncoder, Clock clock) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        Account account = accountRepository.findByLoginId(request.getLoginId().trim())
                .filter(found -> matches(request.getPassword(), found.getPasswordHash()))
                .filter(found -> found.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(InvalidCredentialsException::new);

        LocalDateTime now = now();
        AuthSession session = new AuthSession(
                UUID.randomUUID().toString(),
                account,
                now,
                now.plusHours(SESSION_HOURS)
        );
        sessions.put(session.getToken(), session);

        return LoginResponse.from(session);
    }

    public void logout(String authorizationHeader) {
        AuthSession session = resolveSession(authorizationHeader);
        session.expire();
        sessions.remove(session.getToken());
    }

    public AuthAccountResponse me(String authorizationHeader) {
        return AuthAccountResponse.from(resolveSession(authorizationHeader));
    }

    public AuthSession require(String authorizationHeader) {
        return resolveSession(authorizationHeader);
    }

    public AuthSession requireRole(String authorizationHeader, UserRole... allowedRoles) {
        AuthSession session = resolveSession(authorizationHeader);
        if (allowedRoles == null || allowedRoles.length == 0) {
            return session;
        }
        boolean allowed = Arrays.asList(allowedRoles).contains(session.getRole());
        if (!allowed) {
            throw new AccessDeniedException();
        }
        return session;
    }

    private AuthSession resolveSession(String authorizationHeader) {
        String token = extractBearerToken(authorizationHeader);
        AuthSession session = sessions.get(token);
        if (session == null || !session.isAvailable(now())) {
            throw new AuthenticationRequiredException();
        }
        return session;
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.trim().isEmpty()) {
            throw new AuthenticationRequiredException();
        }
        String normalizedHeader = authorizationHeader.trim();
        if (!normalizedHeader.startsWith("Bearer ")) {
            throw new AuthenticationRequiredException();
        }
        String token = normalizedHeader.substring("Bearer ".length()).trim();
        if (token.isEmpty()) {
            throw new AuthenticationRequiredException();
        }
        return token;
    }

    private boolean matches(String password, String passwordHash) {
        return password != null
                && passwordHash != null
                && passwordEncoder.matches(password.trim(), passwordHash.trim());
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("login request is required");
        }
        if (request.getLoginId() == null || request.getLoginId().trim().isEmpty()) {
            throw new IllegalArgumentException("loginId is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
