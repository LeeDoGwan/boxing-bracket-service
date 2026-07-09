package com.boxing.bracket.auth.web;

import com.boxing.bracket.auth.service.AuthService;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AuthInterceptorTest {

    @Mock
    private AuthService authService;

    @Test
    void preHandleSkipsPublicApi() {
        AuthInterceptor interceptor = new AuthInterceptor(authService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/home");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        then(authService).shouldHaveNoInteractions();
    }

    @Test
    void preHandleRequiresAuthenticatedSessionForMe() {
        AuthInterceptor interceptor = new AuthInterceptor(authService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.addHeader("Authorization", "Bearer token-1");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        then(authService).should().require("Bearer token-1");
        then(authService).should(never()).requireRole("Bearer token-1");
    }

    @Test
    void preHandleRequiresServiceManagerForAdminAccount() {
        AuthInterceptor interceptor = new AuthInterceptor(authService);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/admin/accounts");
        request.addHeader("Authorization", "Bearer token-1");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        then(authService).should().requireRole("Bearer token-1", UserRole.SERVICE_MANAGER);
    }

    @Test
    void preHandleRequiresRingManagerForRingManagerApi() {
        AuthInterceptor interceptor = new AuthInterceptor(authService);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ring-manager/bouts/1/start");
        request.addHeader("Authorization", "Bearer token-1");

        boolean result = interceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertThat(result).isTrue();
        then(authService).should().requireRole("Bearer token-1", UserRole.RING_MANAGER);
    }
}
