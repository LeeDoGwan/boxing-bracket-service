package com.boxing.bracket.auth.controller;

import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.auth.exception.AuthenticationRequiredException;
import com.boxing.bracket.auth.service.AuthService;
import com.boxing.bracket.auth.web.AuthInterceptor;
import com.boxing.bracket.auth.web.AuthWebConfig;
import com.boxing.bracket.user.admin.controller.AdminAccountController;
import com.boxing.bracket.user.admin.service.AdminAccountService;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAccountController.class)
@Import({AuthWebConfig.class, AuthInterceptor.class})
@TestPropertySource(properties = "boxing.auth.enabled=true")
class AuthProtectedApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminAccountService adminAccountService;

    @MockBean
    private AuthService authService;

    @Test
    void protectedAdminApiRejectsMissingAuthentication() throws Exception {
        willThrow(new AuthenticationRequiredException())
                .given(authService)
                .requireRole(null, UserRole.SERVICE_MANAGER);

        mockMvc.perform(get("/api/admin/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void protectedAdminApiRejectsDifferentRole() throws Exception {
        willThrow(new AccessDeniedException())
                .given(authService)
                .requireRole("Bearer judge-token", UserRole.SERVICE_MANAGER);

        mockMvc.perform(get("/api/admin/accounts")
                        .header("Authorization", "Bearer judge-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
