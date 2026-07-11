package com.boxing.bracket.audit.repository;

import com.boxing.bracket.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    boolean existsByDeduplicationKey(String deduplicationKey);
}
