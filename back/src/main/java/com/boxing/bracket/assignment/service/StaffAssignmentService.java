package com.boxing.bracket.assignment.service;

import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.assignment.dto.AssignedRingResponse;
import com.boxing.bracket.assignment.dto.StaffAssignmentActiveRequest;
import com.boxing.bracket.assignment.dto.StaffAssignmentRequest;
import com.boxing.bracket.assignment.dto.StaffAssignmentResponse;
import com.boxing.bracket.assignment.exception.StaffAssignmentNotFoundException;
import com.boxing.bracket.assignment.repository.StaffAssignmentRepository;
import com.boxing.bracket.auth.domain.AuthSession;
import com.boxing.bracket.auth.domain.AuthSessionContext;
import com.boxing.bracket.auth.exception.AccessDeniedException;
import com.boxing.bracket.auth.exception.AuthenticationRequiredException;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.ringmanager.dto.RingManagerBoutResponse;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import com.boxing.bracket.user.domain.Account;
import com.boxing.bracket.user.domain.AccountStatus;
import com.boxing.bracket.user.domain.UserRole;
import com.boxing.bracket.user.exception.AccountNotFoundException;
import com.boxing.bracket.user.repository.AccountRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class StaffAssignmentService {

    private final StaffAssignmentRepository assignmentRepository;
    private final AccountRepository accountRepository;
    private final TournamentRepository tournamentRepository;
    private final RingRepository ringRepository;
    private final BoutRepository boutRepository;

    public StaffAssignmentService(
            StaffAssignmentRepository assignmentRepository,
            AccountRepository accountRepository,
            TournamentRepository tournamentRepository,
            RingRepository ringRepository,
            BoutRepository boutRepository
    ) {
        this.assignmentRepository = assignmentRepository;
        this.accountRepository = accountRepository;
        this.tournamentRepository = tournamentRepository;
        this.ringRepository = ringRepository;
        this.boutRepository = boutRepository;
    }

    @Transactional(readOnly = true)
    public List<StaffAssignmentResponse> getAssignments(
            Long tournamentId,
            Long accountId,
            UserRole role,
            Boolean active
    ) {
        return assignmentRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(item -> tournamentId == null || tournamentId.equals(item.getTournamentId()))
                .filter(item -> accountId == null || accountId.equals(item.getAccountId()))
                .filter(item -> role == null || role == item.getRole())
                .filter(item -> active == null || active.equals(item.isActive()))
                .map(StaffAssignmentResponse::from)
                .collect(Collectors.toList());
    }

    public StaffAssignmentResponse createAssignment(StaffAssignmentRequest request) {
        validateRequest(request);
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(AccountNotFoundException::new);
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("account must be ACTIVE");
        }
        if (account.getRole() != request.getRole()) {
            throw new IllegalArgumentException("ACCOUNT_ROLE_MISMATCH");
        }
        if (!tournamentRepository.existsById(request.getTournamentId())) {
            throw new TournamentNotFoundException();
        }
        Ring ring = ringRepository.findById(request.getRingId())
                .orElseThrow(RingNotFoundException::new);
        if (!request.getTournamentId().equals(ring.getTournamentId())) {
            throw new IllegalArgumentException("RING_TOURNAMENT_MISMATCH");
        }
        if (assignmentRepository.existsByAccountIdAndTournamentIdAndRingId(
                request.getAccountId(), request.getTournamentId(), request.getRingId())) {
            throw new com.boxing.bracket.common.exception.WorkflowConflictException("ASSIGNMENT_ALREADY_EXISTS");
        }

        StaffAssignment assignment = StaffAssignment.builder()
                .accountId(request.getAccountId())
                .tournamentId(request.getTournamentId())
                .ringId(request.getRingId())
                .role(request.getRole())
                .active(true)
                .build();
        return StaffAssignmentResponse.from(assignmentRepository.save(assignment));
    }

    public StaffAssignmentResponse changeActive(Long assignmentId, StaffAssignmentActiveRequest request) {
        if (assignmentId == null) {
            throw new IllegalArgumentException("assignmentId is required");
        }
        if (request == null || request.getActive() == null) {
            throw new IllegalArgumentException("active is required");
        }
        StaffAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(StaffAssignmentNotFoundException::new);
        assignment.changeActive(request.getActive());
        return StaffAssignmentResponse.from(assignmentRepository.save(assignment));
    }

    @Transactional(readOnly = true)
    public List<AssignedRingResponse> getAssignedRings(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        AuthSession session = AuthSessionContext.get();
        if (session == null) {
            throw new AuthenticationRequiredException();
        }
        UserRole role = session.getRole();
        if (!StaffAssignment.isRingScopedRole(role)) {
            throw new AccessDeniedException("RING_ACCESS_DENIED");
        }
        return assignmentRepository.findByAccountIdAndTournamentIdAndRoleAndActiveTrueOrderByRingIdAsc(
                        session.getAccountId(), tournamentId, role
                ).stream()
                .map(item -> ringRepository.findById(item.getRingId()).orElse(null))
                .filter(ring -> ring != null)
                .map(AssignedRingResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RingManagerBoutResponse> getAssignedBouts(Long ringId) {
        requireRingAccess(ringId);
        return boutRepository.findByRingIdOrderByScheduledOrderAsc(ringId).stream()
                .filter(bout -> !bout.isEventBout())
                .map(RingManagerBoutResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public void requireBoutAccess(Long boutId) {
        AuthSession session = AuthSessionContext.get();
        if (session == null) {
            return;
        }
        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);
        requireRingAccess(session, bout.getRingId());
    }

    @Transactional(readOnly = true)
    public void requireRingAccess(Long ringId) {
        AuthSession session = AuthSessionContext.get();
        if (session == null) {
            return;
        }
        requireRingAccess(session, ringId);
    }

    @Transactional(readOnly = true)
    public void requireRingAccess(AuthSession session, Long ringId) {
        if (ringId == null) {
            throw new IllegalArgumentException("ringId is required");
        }
        Ring ring = ringRepository.findById(ringId)
                .orElseThrow(RingNotFoundException::new);
        if (session.getRole() == UserRole.SERVICE_MANAGER) {
            return;
        }
        if (!StaffAssignment.isRingScopedRole(session.getRole())
                || !assignmentRepository.existsByAccountIdAndTournamentIdAndRingIdAndRoleAndActiveTrue(
                session.getAccountId(), ring.getTournamentId(), ringId, session.getRole())) {
            throw new AccessDeniedException("RING_ACCESS_DENIED");
        }
    }

    private void validateRequest(StaffAssignmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("assignment request is required");
        }
        if (!StaffAssignment.isRingScopedRole(request.getRole())) {
            throw new IllegalArgumentException("role must be JUDGE, SUPERVISOR, or RING_MANAGER");
        }
    }
}
