package com.boxing.bracket.audit.controller;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.audit.dto.AuditLogPageResponse;
import com.boxing.bracket.audit.dto.AuditLogSearchCondition;
import com.boxing.bracket.audit.service.AuditLogService;
import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.user.domain.UserRole;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<AuditLogPageResponse> getAuditLogs(
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long actorAccountId,
            @RequestParam(required = false) UserRole actorRole,
            @RequestParam(required = false) AuditActionType actionType,
            @RequestParam(required = false) AuditTargetType targetType,
            @RequestParam(required = false) Long ringId,
            @RequestParam(required = false) Long boutId,
            @RequestParam(required = false) Boolean success,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        AuditLogSearchCondition condition = new AuditLogSearchCondition(
                tournamentId,
                actorAccountId,
                actorRole,
                actionType,
                targetType,
                ringId,
                boutId,
                success,
                from,
                to,
                page,
                size
        );
        return ApiResponse.success(auditLogService.getAuditLogs(condition), "OK");
    }
}
