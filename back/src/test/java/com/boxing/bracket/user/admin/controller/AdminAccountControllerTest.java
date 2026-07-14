package com.boxing.bracket.user.admin.controller;

import com.boxing.bracket.user.admin.dto.AdminAccountRequest;
import com.boxing.bracket.user.admin.dto.AdminAccountResponse;
import com.boxing.bracket.user.admin.service.AdminAccountService;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.exception.AccountNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAccountController.class)
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminAccountService adminAccountService;

    @Test
    void getAccountsReturnsAccountList() throws Exception {
        given(adminAccountService.getAccounts(null, null, null)).willReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].accountId").value(1))
                .andExpect(jsonPath("$.data[0].loginId").value("judge01"));
    }

    @Test
    void getAccountsPassesSearchFilters() throws Exception {
        given(adminAccountService.getAccounts("ring", UserRole.RING_MANAGER, AccountStatus.INACTIVE))
                .willReturn(List.of(response(1L)));

        mockMvc.perform(get("/api/admin/accounts")
                        .param("keyword", "ring")
                        .param("role", "RING_MANAGER")
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].accountId").value(1));
    }

    @Test
    void getAccountReturnsAccount() throws Exception {
        given(adminAccountService.getAccount(1L)).willReturn(response(1L));

        mockMvc.perform(get("/api/admin/accounts/{accountId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(1));
    }

    @Test
    void getAccountReturnsNotFoundForMissingAccount() throws Exception {
        given(adminAccountService.getAccount(99L)).willThrow(new AccountNotFoundException());

        mockMvc.perform(get("/api/admin/accounts/{accountId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void createAccountReturnsCreatedAccount() throws Exception {
        AdminAccountRequest request = request();
        given(adminAccountService.createAccount(any(AdminAccountRequest.class))).willReturn(response(1L));

        mockMvc.perform(post("/api/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(1))
                .andExpect(jsonPath("$.data.loginId").value("judge01"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void createAccountReturnsBadRequestForMissingLoginId() throws Exception {
        AdminAccountRequest request = new AdminAccountRequest(" ", "hash1", "Judge One", UserRole.JUDGE, null);

        mockMvc.perform(post("/api/admin/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("loginId is required"));
    }

    @Test
    void updateAccountReturnsUpdatedAccount() throws Exception {
        AdminAccountRequest request = request();
        given(adminAccountService.updateAccount(eq(1L), any(AdminAccountRequest.class))).willReturn(response(1L));

        mockMvc.perform(put("/api/admin/accounts/{accountId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountId").value(1));
    }

    @Test
    void updateAccountReturnsNotFoundForMissingAccount() throws Exception {
        AdminAccountRequest request = request();
        given(adminAccountService.updateAccount(eq(99L), any(AdminAccountRequest.class)))
                .willThrow(new AccountNotFoundException());

        mockMvc.perform(put("/api/admin/accounts/{accountId}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    @Test
    void deleteAccountReturnsOk() throws Exception {
        mockMvc.perform(delete("/api/admin/accounts/{accountId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"));
    }

    @Test
    void deleteAccountReturnsNotFoundForMissingAccount() throws Exception {
        willThrow(new AccountNotFoundException()).given(adminAccountService).deleteAccount(99L);

        mockMvc.perform(delete("/api/admin/accounts/{accountId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account not found"));
    }

    private AdminAccountRequest request() {
        return new AdminAccountRequest("judge01", "hash1", "Judge One", UserRole.JUDGE, AccountStatus.ACTIVE);
    }

    private AdminAccountResponse response(Long id) {
        Account account = Account.builder()
                .loginId("judge01")
                .passwordHash("hash1")
                .name("Judge One")
                .role(UserRole.JUDGE)
                .status(AccountStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return AdminAccountResponse.from(account);
    }
}
