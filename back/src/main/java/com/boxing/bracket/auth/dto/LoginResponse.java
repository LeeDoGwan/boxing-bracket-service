package com.boxing.bracket.auth.dto;

import com.boxing.bracket.auth.domain.AuthSession;

import java.time.LocalDateTime;

public class LoginResponse {

    private final String tokenType;
    private final String accessToken;
    private final LocalDateTime expiresAt;
    private final AuthAccountResponse account;

    private LoginResponse(String tokenType, String accessToken, LocalDateTime expiresAt, AuthAccountResponse account) {
        this.tokenType = tokenType;
        this.accessToken = accessToken;
        this.expiresAt = expiresAt;
        this.account = account;
    }

    public static LoginResponse from(AuthSession session) {
        return new LoginResponse(
                "Bearer",
                session.getToken(),
                session.getExpiresAt(),
                AuthAccountResponse.from(session)
        );
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public AuthAccountResponse getAccount() {
        return account;
    }
}
