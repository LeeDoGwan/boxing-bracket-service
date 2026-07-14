package com.boxing.bracket.audit.dto;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditLog;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.user.domain.UserRole;

import java.time.LocalDateTime;

public class AuditLogResponse {

    private final Long id;
    private final Long tournamentId;
    private final Long actorAccountId;
    private final String actorUsername;
    private final UserRole actorRole;
    private final AuditActionType actionType;
    private final AuditTargetType targetType;
    private final Long targetId;
    private final Long ringId;
    private final Long boutId;
    private final String beforeData;
    private final String afterData;
    private final String ipAddress;
    private final String userAgent;
    private final boolean success;
    private final String failureReason;
    private final LocalDateTime createdAt;

    private AuditLogResponse(AuditLog auditLog) {
        this.id = auditLog.getId();
        this.tournamentId = auditLog.getTournamentId();
        this.actorAccountId = auditLog.getActorAccountId();
        this.actorUsername = auditLog.getActorUsername();
        this.actorRole = auditLog.getActorRole();
        this.actionType = auditLog.getActionType();
        this.targetType = auditLog.getTargetType();
        this.targetId = auditLog.getTargetId();
        this.ringId = auditLog.getRingId();
        this.boutId = auditLog.getBoutId();
        this.beforeData = auditLog.getBeforeData();
        this.afterData = auditLog.getAfterData();
        this.ipAddress = auditLog.getIpAddress();
        this.userAgent = auditLog.getUserAgent();
        this.success = auditLog.isSuccess();
        this.failureReason = auditLog.getFailureReason();
        this.createdAt = auditLog.getCreatedAt();
    }

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(auditLog);
    }

    public Long getId() { return id; }
    public Long getTournamentId() { return tournamentId; }
    public Long getActorAccountId() { return actorAccountId; }
    public String getActorUsername() { return actorUsername; }
    public UserRole getActorRole() { return actorRole; }
    public AuditActionType getActionType() { return actionType; }
    public AuditTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public Long getRingId() { return ringId; }
    public Long getBoutId() { return boutId; }
    public String getBeforeData() { return beforeData; }
    public String getAfterData() { return afterData; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public boolean isSuccess() { return success; }
    public String getFailureReason() { return failureReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
