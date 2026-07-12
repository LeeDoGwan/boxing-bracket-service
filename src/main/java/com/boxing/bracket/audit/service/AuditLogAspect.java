package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.user.domain.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);

    private final AuditActionResolver actionResolver;
    private final AuditStateSnapshotService snapshotService;
    private final AuditDataSerializer serializer;
    private final AuditLogService auditLogService;

    public AuditLogAspect(
            AuditActionResolver actionResolver,
            AuditStateSnapshotService snapshotService,
            AuditDataSerializer serializer,
            AuditLogService auditLogService
    ) {
        this.actionResolver = actionResolver;
        this.snapshotService = snapshotService;
        this.serializer = serializer;
        this.auditLogService = auditLogService;
    }

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object recordControllerMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        AuditOperation operation = actionResolver.resolve(request.getMethod(), request.getRequestURI()).orElse(null);
        if (operation == null) {
            return joinPoint.proceed();
        }

        AuditSnapshot snapshot = captureSafely(request.getRequestURI());
        JsonNode requestData = serializer.toRequestNode(joinPoint.getArgs());
        String beforeData = snapshot.getBeforeData() == null
                ? serializer.serializeRequestArguments(joinPoint.getArgs())
                : snapshot.getBeforeData();

        try {
            Object result = joinPoint.proceed();
            Object responseData = unwrap(result);
            JsonNode responseNode = responseData == null ? null : serializer.toNode(responseData);
            AuditActor actor = resolveActor(operation, responseNode, requestData);
            AuditIdentifiers identifiers = identify(operation, snapshot, requestData, responseNode);
            String afterData = serializer.serialize(responseData);

            auditLogService.recordSafely(command(
                    operation,
                    actor,
                    identifiers,
                    beforeData,
                    afterData,
                    request,
                    true,
                    null
            ));
            return result;
        } catch (Throwable exception) {
            AuditOperation failureOperation = failedOperation(operation);
            AuditActor actor = resolveActor(failureOperation, null, requestData);
            AuditIdentifiers identifiers = identify(failureOperation, snapshot, requestData, null);

            auditLogService.recordSafely(command(
                    failureOperation,
                    actor,
                    identifiers,
                    beforeData,
                    null,
                    request,
                    false,
                    exception.getClass().getSimpleName()
            ));
            throw exception;
        }
    }

    private AuditSnapshot captureSafely(String requestUri) {
        try {
            return snapshotService.capture(requestUri);
        } catch (RuntimeException exception) {
            log.error("Failed to capture audit snapshot. uri={}", requestUri, exception);
            return AuditSnapshot.empty();
        }
    }

    private AuditOperation failedOperation(AuditOperation operation) {
        if (operation.getActionType() == AuditActionType.LOGIN_SUCCEEDED) {
            return new AuditOperation(AuditActionType.LOGIN_FAILED, operation.getTargetType());
        }
        return operation;
    }

    private Object unwrap(Object result) {
        if (result instanceof ApiResponse) {
            return ((ApiResponse<?>) result).getData();
        }
        return result;
    }

    private AuditActor resolveActor(AuditOperation operation, JsonNode responseData, JsonNode requestData) {
        AuditActor actor = AuditActorContext.get();
        if (actor != null) {
            return actor;
        }
        if (operation.getActionType() == AuditActionType.LOGIN_SUCCEEDED) {
            Long accountId = serializer.findLong(responseData, "accountId");
            String loginId = serializer.findText(responseData, "loginId");
            String role = serializer.findText(responseData, "role");
            return new AuditActor(accountId, loginId, toUserRole(role));
        }
        if (operation.getActionType() == AuditActionType.LOGIN_FAILED) {
            return new AuditActor(null, serializer.findText(requestData, "loginId"), null);
        }
        return null;
    }

    private UserRole toUserRole(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UserRole.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private AuditIdentifiers identify(
            AuditOperation operation,
            AuditSnapshot snapshot,
            JsonNode requestData,
            JsonNode responseData
    ) {
        Long tournamentId = first(
                serializer.findLong(responseData, "tournamentId"),
                serializer.findLong(requestData, "tournamentId"),
                snapshot.getTournamentId()
        );
        Long ringId = first(
                serializer.findLong(responseData, "ringId"),
                serializer.findLong(requestData, "ringId"),
                snapshot.getRingId()
        );
        Long boutId = first(
                serializer.findLong(responseData, "boutId"),
                serializer.findLong(requestData, "boutId"),
                snapshot.getBoutId()
        );
        Long targetId = serializer.findLong(responseData, targetIdField(operation.getTargetType()));
        if (targetId == null && hasSnapshotTarget(operation.getTargetType())) {
            targetId = snapshot.getTargetId();
        }
        return new AuditIdentifiers(tournamentId, ringId, boutId, targetId);
    }

    private String targetIdField(AuditTargetType targetType) {
        switch (targetType) {
            case BOUT:
                return "boutId";
            case RING:
                return "ringId";
            case ROUND_SCORE:
                return "scoreId";
            case PENALTY:
                return "penaltyId";
            case BOUT_RESULT:
                return "resultId";
            case ACCOUNT:
            case AUTH:
                return "accountId";
            case NOTICE:
                return "noticeId";
            default:
                return "id";
        }
    }

    private boolean hasSnapshotTarget(AuditTargetType targetType) {
        return targetType == AuditTargetType.BOUT
                || targetType == AuditTargetType.RING
                || targetType == AuditTargetType.ACCOUNT
                || targetType == AuditTargetType.NOTICE;
    }

    private Long first(Long first, Long second, Long third) {
        return first != null ? first : (second != null ? second : third);
    }

    private AuditLogCommand command(
            AuditOperation operation,
            AuditActor actor,
            AuditIdentifiers identifiers,
            String beforeData,
            String afterData,
            HttpServletRequest request,
            boolean success,
            String failureReason
    ) {
        return AuditLogCommand.builder()
                .tournamentId(identifiers.tournamentId)
                .actorAccountId(actor == null ? null : actor.getAccountId())
                .actorUsername(actor == null ? null : actor.getUsername())
                .actorRole(actor == null ? null : actor.getRole())
                .actionType(operation.getActionType())
                .targetType(operation.getTargetType())
                .targetId(identifiers.targetId)
                .ringId(identifiers.ringId)
                .boutId(identifiers.boutId)
                .deduplicationKey(deduplicationKey(operation, actor, identifiers, afterData))
                .beforeData(beforeData)
                .afterData(afterData)
                .ipAddress(request.getRemoteAddr())
                .userAgent(request.getHeader("User-Agent"))
                .success(success)
                .failureReason(failureReason)
                .build();
    }

    private String deduplicationKey(
            AuditOperation operation,
            AuditActor actor,
            AuditIdentifiers identifiers,
            String afterData
    ) {
        if (!isIdempotent(operation.getActionType()) || afterData == null) {
            return null;
        }
        String value = operation.getActionType().name()
                + "|" + operation.getTargetType().name()
                + "|" + (actor == null ? null : actor.getAccountId())
                + "|" + identifiers.targetId
                + "|" + afterData;
        return sha256(value);
    }

    private boolean isIdempotent(AuditActionType actionType) {
        return actionType == AuditActionType.BOUT_STARTED
                || actionType == AuditActionType.BOUT_STATUS_CHANGED
                || actionType == AuditActionType.ROUND_STARTED
                || actionType == AuditActionType.NEXT_BOUT_READY
                || actionType == AuditActionType.SCORE_SUBMITTED
                || actionType == AuditActionType.RESULT_CONFIRMED;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static class AuditIdentifiers {

        private final Long tournamentId;
        private final Long ringId;
        private final Long boutId;
        private final Long targetId;

        private AuditIdentifiers(Long tournamentId, Long ringId, Long boutId, Long targetId) {
            this.tournamentId = tournamentId;
            this.ringId = ringId;
            this.boutId = boutId;
            this.targetId = targetId;
        }
    }
}
