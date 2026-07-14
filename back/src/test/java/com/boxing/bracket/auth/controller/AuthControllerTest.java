package com.boxing.bracket.auth.controller;

import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.dto.AuthAccountResponse;
import com.boxing.bracket.auth.dto.LoginRequest;
import com.boxing.bracket.auth.dto.LoginResponse;
import com.boxing.bracket.auth.exception.AuthenticationRequiredException;
import com.boxing.bracket.auth.exception.InvalidCredentialsException;
import com.boxing.bracket.auth.service.AuthService;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void loginReturnsAccessToken() throws Exception {
        given(authService.login(any(LoginRequest.class))).willReturn(loginResponse());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("judge01", "password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").value("token-1"))
                .andExpect(jsonPath("$.data.account.loginId").value("judge01"))
                .andExpect(jsonPath("$.data.account.role").value("JUDGE"));
    }

    @Test
    void loginReturnsUnauthorizedForInvalidCredentials() throws Exception {
        given(authService.login(any(LoginRequest.class))).willThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("judge01", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void loginReturnsBadRequestForMissingPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("judge01", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("password is required"));
    }

    @Test
    void logoutReturnsOk() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void logoutReturnsUnauthorizedForMissingToken() throws Exception {
        willThrow(new AuthenticationRequiredException()).given(authService).logout(eq(null));

        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void meReturnsCurrentAccount() throws Exception {
        given(authService.me("Bearer token-1")).willReturn(AuthAccountResponse.from(account()));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer token-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(1))
                .andExpect(jsonPath("$.data.loginId").value("judge01"));
    }

    private LoginResponse loginResponse() {
        AuthSession session = new AuthSession(
                "token-1",
                account(),
                LocalDateTime.of(2026, 7, 10, 0, 0),
                LocalDateTime.of(2026, 7, 10, 12, 0)
        );
        return LoginResponse.from(session);
    }

    private Account account() {
        Account account = Account.builder()
                .loginId("judge01")
                .passwordHash("password1")
                .name("Judge One")
                .role(UserRole.JUDGE)
                .status(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(account, "id", 1L);
        return account;
    }
}
