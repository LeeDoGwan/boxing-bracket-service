package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditLog;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.audit.dto.AuditLogPageResponse;
import com.boxing.bracket.audit.dto.AuditLogSearchCondition;
import com.boxing.bracket.audit.repository.AuditLogRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class AuditLogServiceTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private TournamentRepository tournamentRepository;

    private AuditLogService auditLogService;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(
                new AuditLogWriter(auditLogRepository),
                auditLogRepository,
                tournamentRepository
        );
    }

    @Test
    void filtersAuditLogsAndPaginatesNewestFirst() {
        auditLogRepository.save(log(AuditActionType.BOUT_STARTED, true, 1L));
        auditLogRepository.save(log(AuditActionType.LOGIN_FAILED, false, null));
        auditLogRepository.save(log(AuditActionType.BOUT_STARTED, true, 2L));
        auditLogRepository.flush();

        AuditLogPageResponse response = auditLogService.getAuditLogs(new AuditLogSearchCondition(
                null,
                7L,
                UserRole.RING_MANAGER,
                AuditActionType.BOUT_STARTED,
                AuditTargetType.BOUT,
                4L,
                null,
                true,
                null,
                null,
                0,
                1
        ));

        assertThat(response.getTotalElements()).isEqualTo(2);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getActionType()).isEqualTo(AuditActionType.BOUT_STARTED);
        assertThat(response.getContent().get(0).isSuccess()).isTrue();
    }

    @Test
    void rejectsInvalidRangeAndUnknownTournament() {
        assertThatThrownBy(() -> auditLogService.getAuditLogs(new AuditLogSearchCondition(
                null, null, null, null, null, null, null, null,
                LocalDateTime.now(), LocalDateTime.now().minusMinutes(1), 0, 20
        ))).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> auditLogService.getAuditLogs(new AuditLogSearchCondition(
                999L, null, null, null, null, null, null, null,
                null, null, 0, 20
        ))).isInstanceOf(TournamentNotFoundException.class);
    }

    @Test
    void keepsOneAuditRecordForAnIdempotentOperationFingerprint() {
        AuditLogCommand command = AuditLogCommand.builder()
                .actorAccountId(7L)
                .actorUsername("ring-manager")
                .actorRole(UserRole.RING_MANAGER)
                .actionType(AuditActionType.BOUT_STARTED)
                .targetType(AuditTargetType.BOUT)
                .targetId(11L)
                .boutId(11L)
                .deduplicationKey("same-operation")
                .afterData("{\"boutId\":11,\"status\":\"IN_PROGRESS\"}")
                .success(true)
                .build();

        auditLogService.recordSafely(command);
        auditLogService.recordSafely(command);

        assertThat(auditLogRepository.count()).isEqualTo(1);
    }

    private AuditLog log(AuditActionType actionType, boolean success, Long boutId) {
        return AuditLog.from(AuditLogCommand.builder()
                .tournamentId(boutId == null ? null : 3L)
                .actorAccountId(7L)
                .actorUsername("ring-manager")
                .actorRole(UserRole.RING_MANAGER)
                .actionType(actionType)
                .targetType(actionType == AuditActionType.LOGIN_FAILED ? AuditTargetType.AUTH : AuditTargetType.BOUT)
                .targetId(boutId)
                .ringId(boutId == null ? null : 4L)
                .boutId(boutId)
                .beforeData("{}")
                .afterData("{}")
                .ipAddress("127.0.0.1")
                .userAgent("test")
                .success(success)
                .failureReason(success ? null : "InvalidCredentialsException")
                .build());
    }
}
