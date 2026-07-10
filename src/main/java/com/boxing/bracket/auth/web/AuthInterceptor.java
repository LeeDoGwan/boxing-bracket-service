package com.boxing.bracket.auth.web;

import com.boxing.bracket.auth.service.AuthService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Lazy
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;
    private final RoleAccessPolicy roleAccessPolicy;

    public AuthInterceptor(@Lazy AuthService authService) {
        this.authService = authService;
        this.roleAccessPolicy = new RoleAccessPolicy();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        roleAccessPolicy.findRule(request.getRequestURI())
                .ifPresent(rule -> {
                    String authorizationHeader = request.getHeader("Authorization");
                    if (rule.hasRoleRestriction()) {
                        authService.requireRole(authorizationHeader, rule.getRoles());
                    } else {
                        authService.require(authorizationHeader);
                    }
                });
        return true;
    }
}
