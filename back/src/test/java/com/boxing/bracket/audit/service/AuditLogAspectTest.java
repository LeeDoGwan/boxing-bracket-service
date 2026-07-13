package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.domain.AuditTargetType;
import com.boxing.bracket.common.response.ApiResponse;
import com.boxing.bracket.user.domain.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuditLogAspectTest {

    private final AuditActionResolver actionResolver = mock(AuditActionResolver.class);
    private final AuditStateSnapshotService snapshotService = mock(AuditStateSnapshotService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final AuditLogAspect aspect = new AuditLogAspect(
            actionResolver,
            snapshotService,
            new AuditDataSerializer(new ObjectMapper()),
            auditLogService
    );

    @AfterEach
    void clearContext() {
        AuditActorContext.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsSuccessfulMutationAfterControllerReturns() throws Throwable {
        request("POST", "/api/ring-manager/bouts/22/start");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        given(joinPoint.getArgs()).willReturn(new Object[]{22L});
        given(joinPoint.proceed()).willReturn(ApiResponse.success(Map.of(
                "boutId", 22L,
                "ringId", 4L,
                "status", "IN_PROGRESS"
        )));
        given(actionResolver.resolve("POST", "/api/ring-manager/bouts/22/start"))
                .willReturn(Optional.of(new AuditOperation(AuditActionType.BOUT_STARTED, AuditTargetType.BOUT)));
        given(snapshotService.capture("/api/ring-manager/bouts/22/start"))
                .willReturn(new AuditSnapshot(3L, 4L, 22L, 22L, "{\"status\":\"SCHEDULED\"}"));
        AuditActorContext.set(new AuditActor(7L, "ring-manager", UserRole.RING_MANAGER));

        Object result = aspect.recordControllerMutation(joinPoint);

        ArgumentCaptor<AuditLogCommand> commandCaptor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogService).recordSafely(commandCaptor.capture());
        AuditLogCommand command = commandCaptor.getValue();
        assertThat(result).isInstanceOf(ApiResponse.class);
        assertThat(command.getActionType()).isEqualTo(AuditActionType.BOUT_STARTED);
        assertThat(command.getActorAccountId()).isEqualTo(7L);
        assertThat(command.getTournamentId()).isEqualTo(3L);
        assertThat(command.getBoutId()).isEqualTo(22L);
        assertThat(command.getBeforeData()).contains("SCHEDULED");
        assertThat(command.getAfterData()).contains("IN_PROGRESS");
        assertThat(command.isSuccess()).isTrue();
    }

    @Test
    void recordsFailedLoginWithoutPersistingPassword() throws Throwable {
        request("POST", "/api/auth/login");
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        given(joinPoint.getArgs()).willReturn(new Object[]{Map.of(
                "loginId", "manager01",
                "password", "secret-password"
        )});
        given(joinPoint.proceed()).willThrow(new IllegalArgumentException("Invalid credentials"));
        given(actionResolver.resolve("POST", "/api/auth/login"))
                .willReturn(Optional.of(new AuditOperation(AuditActionType.LOGIN_SUCCEEDED, AuditTargetType.AUTH)));
        given(snapshotService.capture("/api/auth/login")).willReturn(AuditSnapshot.empty());

        assertThatThrownBy(() -> aspect.recordControllerMutation(joinPoint))
                .isInstanceOf(IllegalArgumentException.class);

        ArgumentCaptor<AuditLogCommand> commandCaptor = ArgumentCaptor.forClass(AuditLogCommand.class);
        verify(auditLogService).recordSafely(commandCaptor.capture());
        AuditLogCommand command = commandCaptor.getValue();
        assertThat(command.getActionType()).isEqualTo(AuditActionType.LOGIN_FAILED);
        assertThat(command.getActorUsername()).isEqualTo("manager01");
        assertThat(command.getFailureReason()).isEqualTo("IllegalArgumentException");
        assertThat(command.getBeforeData()).contains("***").doesNotContain("secret-password");
        assertThat(command.isSuccess()).isFalse();
    }

    private void request(String method, String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, requestUri);
        request.addHeader("User-Agent", "audit-test");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
