package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditLog;
import com.boxing.bracket.audit.repository.AuditLogRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(AuditLogCommand command) {
        String deduplicationKey = command.getDeduplicationKey();
        if (deduplicationKey != null && auditLogRepository.existsByDeduplicationKey(deduplicationKey)) {
            return;
        }
        try {
            auditLogRepository.saveAndFlush(AuditLog.from(command));
        } catch (DataIntegrityViolationException exception) {
            if (deduplicationKey != null) {
                return;
            }
            throw exception;
        }
    }
}
