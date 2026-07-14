package com.boxing.bracket.audit.controller;

import com.boxing.bracket.audit.domain.AuditActionType;
import com.boxing.bracket.audit.dto.AuditLogPageResponse;
import com.boxing.bracket.audit.dto.AuditLogSearchCondition;
import com.boxing.bracket.audit.service.AuditLogService;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditLogController.class)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditLogService auditLogService;

    @Test
    void forwardsFiltersAndReturnsPagedAuditLogs() throws Exception {
        given(auditLogService.getAuditLogs(any())).willReturn(AuditLogPageResponse.from(
                new PageImpl<>(List.of(), PageRequest.of(1, 10), 0)
        ));

        mockMvc.perform(get("/api/admin/audit-logs")
                        .param("tournamentId", "3")
                        .param("actorAccountId", "7")
                        .param("actorRole", "RING_MANAGER")
                        .param("actionType", "BOUT_STARTED")
                        .param("success", "true")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));

        ArgumentCaptor<AuditLogSearchCondition> conditionCaptor = ArgumentCaptor.forClass(AuditLogSearchCondition.class);
        verify(auditLogService).getAuditLogs(conditionCaptor.capture());
        AuditLogSearchCondition condition = conditionCaptor.getValue();
        assertThat(condition.getTournamentId()).isEqualTo(3L);
        assertThat(condition.getActorAccountId()).isEqualTo(7L);
        assertThat(condition.getActorRole()).isEqualTo(UserRole.RING_MANAGER);
        assertThat(condition.getActionType()).isEqualTo(AuditActionType.BOUT_STARTED);
        assertThat(condition.getSuccess()).isTrue();
        assertThat(condition.getPage()).isEqualTo(1);
        assertThat(condition.getSize()).isEqualTo(10);
    }
}
