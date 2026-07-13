package com.boxing.bracket.audit.domain;

import com.boxing.bracket.audit.service.AuditLogCommand;
import com.boxing.bracket.common.entity.BaseTimeEntity;
import com.boxing.bracket.user.domain.UserRole;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;

@Getter
@Entity
@Table(
        name = "audit_logs",
        indexes = {
                @Index(name = "idx_audit_logs_tournament_created", columnList = "tournamentId, createdAt"),
                @Index(name = "idx_audit_logs_actor_created", columnList = "actorAccountId, createdAt"),
                @Index(name = "idx_audit_logs_bout_created", columnList = "boutId, createdAt")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLog extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long tournamentId;

    private Long actorAccountId;

    private String actorUsername;

    @Enumerated(EnumType.STRING)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditTargetType targetType;

    private Long targetId;

    private Long ringId;

    private Long boutId;

    @Column(unique = true, length = 64)
    private String deduplicationKey;

    @Lob
    private String beforeData;

    @Lob
    private String afterData;

    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(nullable = false)
    private boolean success;

    @Column(length = 512)
    private String failureReason;

    @Builder
    private AuditLog(
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
        if (actionType == null) {
            throw new IllegalArgumentException("actionType is required");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("targetType is required");
        }
        this.tournamentId = tournamentId;
        this.actorAccountId = actorAccountId;
        this.actorUsername = normalize(actorUsername);
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.ringId = ringId;
        this.boutId = boutId;
        this.deduplicationKey = truncate(deduplicationKey, 64);
        this.beforeData = beforeData;
        this.afterData = afterData;
        this.ipAddress = normalize(ipAddress);
        this.userAgent = truncate(userAgent, 512);
        this.success = success;
        this.failureReason = truncate(failureReason, 512);
    }

    public static AuditLog from(AuditLogCommand command) {
        return AuditLog.builder()
                .tournamentId(command.getTournamentId())
                .actorAccountId(command.getActorAccountId())
                .actorUsername(command.getActorUsername())
                .actorRole(command.getActorRole())
                .actionType(command.getActionType())
                .targetType(command.getTargetType())
                .targetId(command.getTargetId())
                .ringId(command.getRingId())
                .boutId(command.getBoutId())
                .deduplicationKey(command.getDeduplicationKey())
                .beforeData(command.getBeforeData())
                .afterData(command.getAfterData())
                .ipAddress(command.getIpAddress())
                .userAgent(command.getUserAgent())
                .success(command.isSuccess())
                .failureReason(command.getFailureReason())
                .build();
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String truncate(String value, int maxLength) {
        String normalized = normalize(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
