package com.boxing.bracket.audit.dto;

import com.boxing.bracket.audit.domain.AuditLog;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class AuditLogPageResponse {

    private final List<AuditLogResponse> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;

    private AuditLogPageResponse(Page<AuditLog> auditLogs) {
        this.content = auditLogs.getContent().stream()
                .map(AuditLogResponse::from)
                .collect(Collectors.toList());
        this.page = auditLogs.getNumber();
        this.size = auditLogs.getSize();
        this.totalElements = auditLogs.getTotalElements();
        this.totalPages = auditLogs.getTotalPages();
    }

    public static AuditLogPageResponse from(Page<AuditLog> auditLogs) {
        return new AuditLogPageResponse(auditLogs);
    }

    public List<AuditLogResponse> getContent() { return content; }
    public int getPage() { return page; }
    public int getSize() { return size; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
