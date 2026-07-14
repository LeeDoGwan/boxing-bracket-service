package com.boxing.bracket.assignment.repository;

import com.boxing.bracket.assignment.domain.StaffAssignment;
import com.boxing.bracket.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StaffAssignmentRepository extends JpaRepository<StaffAssignment, Long> {

    List<StaffAssignment> findByTournamentIdOrderByIdAsc(Long tournamentId);

    List<StaffAssignment> findByAccountIdOrderByIdAsc(Long accountId);

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
