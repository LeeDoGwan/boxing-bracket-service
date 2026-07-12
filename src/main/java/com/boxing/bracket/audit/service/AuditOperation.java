package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;

public class AuditOperation {

    private final AuditActionType actionType;
    private final AuditTargetType targetType;

    public AuditOperation(AuditActionType actionType, AuditTargetType targetType) {
        this.actionType = actionType;
        this.targetType = targetType;
    }

    public AuditActionType getActionType() {
        return actionType;
    }

    public AuditTargetType getTargetType() {
        return targetType;
    }
}
