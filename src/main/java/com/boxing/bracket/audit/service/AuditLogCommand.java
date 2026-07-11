package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.user.domain.UserRole;
import lombok.Builder;
import lombok.Getter;

@Getter
public class AuditLogCommand {

    private final Long tournamentId;
    private final Long actorAccountId;
    private final String actorUsername;
    private final UserRole actorRole;
    private final AuditActionType actionType;
    private final AuditTargetType targetType;
    private final Long targetId;
    private final Long ringId;
    private final Long boutId;
    private final String deduplicationKey;
    private final String beforeData;
    private final String afterData;
    private final String ipAddress;
    private final String userAgent;
    private final boolean success;
    private final String failureReason;

    @Builder
    public AuditLogCommand(
            Long tournamentId,
            Long actorAccountId,
            String actorUsername,
            UserRole actorRole,
            AuditActionType actionType,
            AuditTargetType targetType,
            Long targetId,
            Long ringId,
            Long boutId,
            String deduplicationKey,
            String beforeData,
            String afterData,
            String ipAddress,
            String userAgent,
            boolean success,
            String failureReason
    ) {
        this.tournamentId = tournamentId;
        this.actorAccountId = actorAccountId;
        this.actorUsername = actorUsername;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.ringId = ringId;
        this.boutId = boutId;
        this.deduplicationKey = deduplicationKey;
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.success = success;
        this.failureReason = failureReason;
    }
}
