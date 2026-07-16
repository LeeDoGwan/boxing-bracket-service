package com.boxing.bracket.scoring.service;

import com.boxing.bracket.bout.domain.BoutSide;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.domain.AuthSessionContext;
import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.Penalty;
import com.boxing.bracket.scoring.dto.PenaltyCreateRequest;
import com.boxing.bracket.scoring.dto.PenaltyResponse;
import com.boxing.bracket.scoring.repository.PenaltyRepository;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SupervisorPenaltyServiceTest {

    @AfterEach
    void clearSession() {
        AuthSessionContext.clear();
    }

    @Mock
    private BoutRepository boutRepository;

    @Mock
    private PenaltyRepository penaltyRepository;

    @InjectMocks
    private SupervisorPenaltyService supervisorPenaltyService;

    @Test
    void createPenaltySavesPenalty() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 1, "warning", 20L);
        given(boutRepository.findById(1L)).willReturn(Optional.of(createBout(BoutStatus.IN_PROGRESS)));
        given(penaltyRepository.save(any(Penalty.class))).willAnswer(invocation -> {
            Penalty penalty = invocation.getArgument(0);
            ReflectionTestUtils.setField(penalty, "id", 100L);
            return penalty;
        });

        PenaltyResponse response = supervisorPenaltyService.createPenalty(1L, request);

        assertThat(response.getPenaltyId()).isEqualTo(100L);
        assertThat(response.getBoutId()).isEqualTo(1L);
        assertThat(response.getTargetSide()).isEqualTo(BoutSide.RED);
        assertThat(response.getPenaltyPoint()).isEqualTo(1);
        assertThat(response.getReason()).isEqualTo("warning");
        assertThat(response.getCreatedBy()).isEqualTo(20L);
    }

    @Test
    void createPenaltyRejectsMissingBout() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.BLUE, 1, "warning", 20L);
        given(boutRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(99L, request))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    @Test
    void createPenaltyRejectsNullTargetSide() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(null, 1, "warning", 20L);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("targetSide is required");
    }

    @Test
    void createPenaltyRejectsNegativePenaltyPoint() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, -1, "warning", 20L);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_PENALTY_VALUE");
    }

    @Test
    void createPenaltyRejectsZeroPenaltyPoint() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 0, "warning", 20L);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_PENALTY_VALUE");
    }

    @Test
    void createPenaltyRejectsAfterResultConfirmation() {
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 1, "warning", 20L);
        given(boutRepository.findById(1L)).willReturn(Optional.of(createBout(BoutStatus.FINISHED)));

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(WorkflowConflictException.class)
                .hasMessage("PENALTY_NOT_ALLOWED");
    }

    @Test
    void createPenaltyUsesAuthenticatedSupervisorWhenRequestActorIsOmitted() {
        AuthSessionContext.set(new AuthSession(
                "token", account(20L), LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1)
        ));
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 1, "warning", null);
        given(boutRepository.findById(1L)).willReturn(Optional.of(createBout(BoutStatus.IN_PROGRESS)));
        given(penaltyRepository.save(any(Penalty.class))).willAnswer(invocation -> invocation.getArgument(0));

        PenaltyResponse response = supervisorPenaltyService.createPenalty(1L, request);

        assertThat(response.getCreatedBy()).isEqualTo(20L);
    }

    @Test
    void createPenaltyRejectsForgedRequestActor() {
        AuthSessionContext.set(new AuthSession(
                "token", account(20L), LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1)
        ));
        PenaltyCreateRequest request = new PenaltyCreateRequest(BoutSide.RED, 1, "warning", 99L);

        assertThatThrownBy(() -> supervisorPenaltyService.createPenalty(1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("ACTOR_ID_MISMATCH");
    }

    @Test
    void getPenaltiesReturnsBoutHistoryInCreationOrder() {
        Penalty first = createPenalty(100L, "warning");
        Penalty second = createPenalty(101L, "holding");
        given(boutRepository.existsById(1L)).willReturn(true);
        given(penaltyRepository.findByBoutIdOrderByCreatedAtAscIdAsc(1L)).willReturn(List.of(first, second));

        List<PenaltyResponse> responses = supervisorPenaltyService.getPenalties(1L);

        assertThat(responses).extracting(PenaltyResponse::getPenaltyId).containsExactly(100L, 101L);
        assertThat(responses.get(1).getReason()).isEqualTo("holding");
    }

    @Test
    void getPenaltiesRejectsMissingBout() {
        given(boutRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> supervisorPenaltyService.getPenalties(99L))
                .isInstanceOf(BoutNotFoundException.class)
                .hasMessage("Bout not found");
    }

    private Penalty createPenalty(Long id, String reason) {
        Penalty penalty = Penalty.builder()
                .boutId(1L)
                .targetSide(BoutSide.RED)
                .penaltyPoint(1)
                .reason(reason)
                .createdBy(20L)
                .build();
        ReflectionTestUtils.setField(penalty, "id", id);
        return penalty;
    }

    private Bout createBout(BoutStatus status) {
        return Bout.builder()
                .tournamentId(1L)
                .ringId(1L)
                .boutNumber(1)
                .redAthleteId(10L)
                .blueAthleteId(11L)
                .status(status)
                .build();
    }

    private Account account(Long id) {
        Account account = Account.builder()
                .loginId("supervisor-" + id)
                .passwordHash("hash")
                .name("Supervisor " + id)
                .role(UserRole.SUPERVISOR)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }
}
