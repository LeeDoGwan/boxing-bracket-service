package com.boxing.bracket.auth.web;

import com.boxing.bracket.audit.service.AuditActor;
import com.boxing.bracket.audit.service.AuditActorContext;
import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.domain.AuthSessionContext;
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
        AuditActorContext.clear();
        AuthSessionContext.clear();
        roleAccessPolicy.findRule(request.getRequestURI())
                .ifPresent(rule -> {
                    String authorizationHeader = request.getHeader("Authorization");
                    AuthSession session;
                    if (rule.hasRoleRestriction()) {
                        session = authService.requireRole(authorizationHeader, rule.getRoles());
                    } else {
                        session = authService.require(authorizationHeader);
                    }
                    if (session != null) {
                        AuditActorContext.set(AuditActor.from(session));
                        AuthSessionContext.set(session);
                    }
                });
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        AuditActorContext.clear();
        AuthSessionContext.clear();
    }
}
