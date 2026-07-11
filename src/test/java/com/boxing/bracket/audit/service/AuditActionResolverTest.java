package com.boxing.bracket.audit.service;

import com.boxing.bracket.audit.domain.AuditActionType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditActionResolverTest {

    private final AuditActionResolver resolver = new AuditActionResolver();

    @Test
    void resolvesRoundStartBeforeGenericBoutStart() {
        AuditOperation operation = resolver.resolve(
                "POST",
                "/api/ring-manager/bouts/11/rounds/2/start"
        ).orElseThrow();

        assertThat(operation.getActionType()).isEqualTo(AuditActionType.ROUND_STARTED);
    }
}
