package com.boxing.bracket.audit.dto;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.user.domain.UserRole;

import java.time.LocalDateTime;

public class AuditLogSearchCondition {

    private final Long tournamentId;
    private final Long actorAccountId;
    private final UserRole actorRole;
    private final AuditActionType actionType;
    private final AuditTargetType targetType;
    private final Long ringId;
    private final Long boutId;
    private final Boolean success;
    private final LocalDateTime from;
    private final LocalDateTime to;
    private final int page;
    private final int size;

    public AuditLogSearchCondition(
            Long tournamentId,
            Long actorAccountId,
            UserRole actorRole,
            AuditActionType actionType,
            AuditTargetType targetType,
            Long ringId,
            Long boutId,
            Boolean success,
            LocalDateTime from,
            LocalDateTime to,
            int page,
            int size
    ) {
        this.tournamentId = tournamentId;
        this.actorAccountId = actorAccountId;
        this.actorRole = actorRole;
        this.actionType = actionType;
        this.targetType = targetType;
        this.ringId = ringId;
        this.boutId = boutId;
        this.success = success;
        this.from = from;
        this.to = to;
        this.page = page;
        this.size = size;
    }

    public Long getTournamentId() { return tournamentId; }
    public Long getActorAccountId() { return actorAccountId; }
    public UserRole getActorRole() { return actorRole; }
    public AuditActionType getActionType() { return actionType; }
    public AuditTargetType getTargetType() { return targetType; }
    public Long getRingId() { return ringId; }
    public Long getBoutId() { return boutId; }
    public Boolean getSuccess() { return success; }
    public LocalDateTime getFrom() { return from; }
    public LocalDateTime getTo() { return to; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}
