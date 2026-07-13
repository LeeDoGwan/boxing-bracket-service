package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditActionResolver {

    public Optional<AuditOperation> resolve(String method, String requestUri) {
        if (method == null || requestUri == null) {
            return Optional.empty();
        }

        if ("POST".equals(method) && "/api/auth/login".equals(requestUri)) {
            return Optional.of(operation(AuditActionType.LOGIN_SUCCEEDED, AuditTargetType.AUTH));
        }
        if ("POST".equals(method) && "/api/auth/logout".equals(requestUri)) {
            return Optional.of(operation(AuditActionType.LOGOUT, AuditTargetType.AUTH));
        }
        if (requestUri.startsWith("/api/ring-manager/bouts/")) {
            if ("POST".equals(method) && requestUri.contains("/rounds/") && requestUri.endsWith("/start")) {
                return Optional.of(operation(AuditActionType.ROUND_STARTED, AuditTargetType.BOUT));
            }
            if ("POST".equals(method) && requestUri.endsWith("/start")) {
                return Optional.of(operation(AuditActionType.BOUT_STARTED, AuditTargetType.BOUT));
            }
            if ("POST".equals(method) && requestUri.endsWith("/status")) {
                return Optional.of(operation(AuditActionType.BOUT_STATUS_CHANGED, AuditTargetType.BOUT));
            }
        }
        if ("POST".equals(method) && requestUri.startsWith("/api/ring-manager/rings/") && requestUri.endsWith("/next")) {
            return Optional.of(operation(AuditActionType.NEXT_BOUT_READY, AuditTargetType.RING));
        }
        if ("POST".equals(method) && requestUri.startsWith("/api/judge/bouts/") && requestUri.endsWith("/scores")) {
            return Optional.of(operation(AuditActionType.SCORE_SUBMITTED, AuditTargetType.ROUND_SCORE));
        }
        if ("POST".equals(method) && requestUri.startsWith("/api/supervisor/bouts/") && requestUri.endsWith("/penalties")) {
            return Optional.of(operation(AuditActionType.PENALTY_CREATED, AuditTargetType.PENALTY));
        }
        if ("POST".equals(method) && requestUri.startsWith("/api/supervisor/bouts/") && requestUri.endsWith("/result")) {
            return Optional.of(operation(AuditActionType.RESULT_CONFIRMED, AuditTargetType.BOUT_RESULT));
        }
        if (requestUri.startsWith("/api/admin/bouts")) {
            if ("POST".equals(method) && requestUri.endsWith("/import")) {
                return Optional.of(operation(AuditActionType.BOUT_IMPORTED, AuditTargetType.BOUT));
            }
            return mutation(method, AuditActionType.BOUT_CREATED, AuditActionType.BOUT_UPDATED, AuditActionType.BOUT_DELETED, AuditTargetType.BOUT);
        }
        if (requestUri.startsWith("/api/admin/accounts")) {
            return mutation(method, AuditActionType.ACCOUNT_CREATED, AuditActionType.ACCOUNT_UPDATED, AuditActionType.ACCOUNT_DELETED, AuditTargetType.ACCOUNT);
        }
        if (requestUri.startsWith("/api/admin/notices")) {
            return mutation(method, AuditActionType.NOTICE_CREATED, AuditActionType.NOTICE_UPDATED, AuditActionType.NOTICE_DELETED, AuditTargetType.NOTICE);
        }
        if (requestUri.startsWith("/api/admin/schedules")) {
            return mutation(method, AuditActionType.SCHEDULE_CREATED, AuditActionType.SCHEDULE_UPDATED, AuditActionType.SCHEDULE_DELETED, AuditTargetType.SCHEDULE);
        }
        return Optional.empty();
    }

    private Optional<AuditOperation> mutation(
            String method,
            AuditActionType created,
            AuditActionType updated,
            AuditActionType deleted,
            AuditTargetType targetType
    ) {
        if ("POST".equals(method)) {
            return Optional.of(operation(created, targetType));
        }
        if ("PUT".equals(method)) {
            return Optional.of(operation(updated, targetType));
        }
        if ("DELETE".equals(method)) {
            return Optional.of(operation(deleted, targetType));
        }
        return Optional.empty();
    }

    private AuditOperation operation(AuditActionType actionType, AuditTargetType targetType) {
        return new AuditOperation(actionType, targetType);
    }
}
