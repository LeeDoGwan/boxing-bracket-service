package com.boxing.bracket.assignment.service;

import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.assignment.dto.StaffAssignmentRequest;
import com.boxing.bracket.assignment.dto.StaffAssignmentResponse;
import com.boxing.bracket.assignment.repository.StaffAssignmentRepository;
import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.domain.AuthSessionContext;
import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.domain.RingStatus;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StaffAssignmentServiceTest {

    @Mock
    private StaffAssignmentRepository assignmentRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private RingRepository ringRepository;

    @Mock
    private BoutRepository boutRepository;

    @AfterEach
    void clearSession() {
        AuthSessionContext.clear();
    }

    @Test
    void createsAssignmentForMatchingActiveAccountAndRing() {
        StaffAssignmentService service = service();
        Account account = account(10L, UserRole.JUDGE, AccountStatus.ACTIVE);
        Ring ring = ring(20L, 1L);
        given(accountRepository.findById(10L)).willReturn(Optional.of(account));
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(20L)).willReturn(Optional.of(ring));
        given(assignmentRepository.existsByAccountIdAndTournamentIdAndRingId(10L, 1L, 20L)).willReturn(false);
        given(assignmentRepository.save(any(StaffAssignment.class))).willAnswer(invocation -> {
            StaffAssignment assignment = invocation.getArgument(0);
            ReflectionTestUtils.setField(assignment, "id", 100L);
            return assignment;
        });

        StaffAssignmentResponse response = service.createAssignment(
                new StaffAssignmentRequest(10L, 1L, 20L, UserRole.JUDGE)
        );

        assertThat(response.getAssignmentId()).isEqualTo(100L);
        assertThat(response.getRole()).isEqualTo(UserRole.JUDGE);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void rejectsRoleMismatch() {
        StaffAssignmentService service = service();
        given(accountRepository.findById(10L)).willReturn(
                Optional.of(account(10L, UserRole.SUPERVISOR, AccountStatus.ACTIVE))
        );

        assertThatThrownBy(() -> service.createAssignment(
                new StaffAssignmentRequest(10L, 1L, 20L, UserRole.JUDGE)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("ACCOUNT_ROLE_MISMATCH");
        verify(tournamentRepository, never()).existsById(any());
    }

    @Test
    void rejectsRingFromAnotherTournament() {
        StaffAssignmentService service = service();
        given(accountRepository.findById(10L)).willReturn(
                Optional.of(account(10L, UserRole.JUDGE, AccountStatus.ACTIVE))
        );
        given(tournamentRepository.existsById(1L)).willReturn(true);
        given(ringRepository.findById(20L)).willReturn(Optional.of(ring(20L, 2L)));

        assertThatThrownBy(() -> service.createAssignment(
                new StaffAssignmentRequest(10L, 1L, 20L, UserRole.JUDGE)
        )).isInstanceOf(IllegalArgumentException.class).hasMessage("RING_TOURNAMENT_MISMATCH");
    }

    @Test
    void blocksUnassignedRingImmediately() {
        StaffAssignmentService service = service();
        Account account = account(10L, UserRole.JUDGE, AccountStatus.ACTIVE);
        AuthSessionContext.set(new AuthSession(
                "token", account, LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1)
        ));
        given(ringRepository.findById(20L)).willReturn(Optional.of(ring(20L, 1L)));
        given(assignmentRepository.existsByAccountIdAndTournamentIdAndRingIdAndRoleAndActiveTrue(
                10L, 1L, 20L, UserRole.JUDGE
        )).willReturn(false);

        assertThatThrownBy(() -> service.requireRingAccess(20L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("RING_ACCESS_DENIED");
    }

    private StaffAssignmentService service() {
        return new StaffAssignmentService(
                assignmentRepository,
                accountRepository,
                tournamentRepository,
                ringRepository,
                boutRepository
        );
    }

    private Account account(Long id, UserRole role, AccountStatus status) {
        Account account = Account.builder()
                .loginId("account-" + id)
                .passwordHash("hash")
                .name("Account " + id)
                .role(role)
                .status(status)
                .build();
        ReflectionTestUtils.setField(account, "id", id);
        return account;
    }

    private Ring ring(Long id, Long tournamentId) {
        Ring ring = Ring.builder()
                .tournamentId(tournamentId)
                .name("Ring " + id)
                .status(RingStatus.READY)
                .build();
        ReflectionTestUtils.setField(ring, "id", id);
        return ring;
    }
}
