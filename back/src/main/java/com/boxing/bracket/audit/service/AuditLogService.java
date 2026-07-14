package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditLog;
import com.boxing.bracket.audit.dto.AuditLogPageResponse;
import com.boxing.bracket.audit.dto.AuditLogSearchCondition;
import com.boxing.bracket.audit.repository.AuditLogRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private static final int MAX_PAGE_SIZE = 100;

    private final AuditLogWriter auditLogWriter;
    private final AuditLogRepository auditLogRepository;
    private final TournamentRepository tournamentRepository;

    public AuditLogService(
            AuditLogWriter auditLogWriter,
            AuditLogRepository auditLogRepository,
            TournamentRepository tournamentRepository
    ) {
        this.auditLogWriter = auditLogWriter;
        this.auditLogRepository = auditLogRepository;
        this.tournamentRepository = tournamentRepository;
    }

    public void recordSafely(AuditLogCommand command) {
        try {
            auditLogWriter.write(command);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to persist audit log. actionType={}, targetType={}",
                    command.getActionType(),
                    command.getTargetType(),
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse getAuditLogs(AuditLogSearchCondition condition) {
        validate(condition);
        if (condition.getTournamentId() != null && !tournamentRepository.existsById(condition.getTournamentId())) {
            throw new TournamentNotFoundException();
        }

        Specification<AuditLog> specification = Specification.where(equal("tournamentId", condition.getTournamentId()))
                .and(equal("actorAccountId", condition.getActorAccountId()))
                .and(equal("actorRole", condition.getActorRole()))
                .and(equal("actionType", condition.getActionType()))
                .and(equal("targetType", condition.getTargetType()))
                .and(equal("ringId", condition.getRingId()))
                .and(equal("boutId", condition.getBoutId()))
                .and(equal("success", condition.getSuccess()))
                .and(from(condition.getFrom()))
                .and(to(condition.getTo()));

        PageRequest pageRequest = PageRequest.of(
                condition.getPage(),
                condition.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<AuditLog> auditLogs = auditLogRepository.findAll(specification, pageRequest);
        return AuditLogPageResponse.from(auditLogs);
    }

    private void validate(AuditLogSearchCondition condition) {
        if (condition == null) {
            throw new IllegalArgumentException("audit log search condition is required");
        }
        if (condition.getPage() < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (condition.getSize() < 1 || condition.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (condition.getFrom() != null && condition.getTo() != null
                && condition.getFrom().isAfter(condition.getTo())) {
            throw new IllegalArgumentException("from must be before or equal to to");
        }
    }

    private Specification<AuditLog> equal(String fieldName, Object value) {
        return (root, query, criteriaBuilder) -> value == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.equal(root.get(fieldName), value);
    }

    private Specification<AuditLog> from(LocalDateTime from) {
        return (root, query, criteriaBuilder) -> from == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), from);
    }

    private Specification<AuditLog> to(LocalDateTime to) {
        return (root, query, criteriaBuilder) -> to == null
                ? criteriaBuilder.conjunction()
                : criteriaBuilder.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), to);
    }
}
