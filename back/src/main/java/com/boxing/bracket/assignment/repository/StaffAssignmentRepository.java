package com.boxing.bracket.assignment.repository;

import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    boolean existsByTournamentId(Long tournamentId);

    List<StaffAssignment> findByTournamentIdOrderByIdAsc(Long tournamentId);

    List<StaffAssignment> findByAccountIdOrderByIdAsc(Long accountId);

    boolean existsByAccountId(Long accountId);

    boolean existsByRingId(Long ringId);

    List<StaffAssignment> findByAccountIdAndTournamentIdAndRoleAndActiveTrueOrderByRingIdAsc(
            Long accountId,
            Long tournamentId,
            UserRole role
    );

    boolean existsByAccountIdAndTournamentIdAndRingId(
            Long accountId,
            Long tournamentId,
            Long ringId
    );

    boolean existsByAccountIdAndTournamentIdAndRingIdAndRoleAndActiveTrue(
            Long accountId,
            Long tournamentId,
            Long ringId,
            UserRole role
    );
}
