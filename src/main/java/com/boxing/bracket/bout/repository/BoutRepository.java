package com.boxing.bracket.bout.repository;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoutRepository extends JpaRepository<Bout, Long> {

    List<Bout> findByTournamentIdOrderByScheduledOrderAsc(Long tournamentId);

    List<Bout> findByRingIdOrderByScheduledOrderAsc(Long ringId);

    Optional<Bout> findFirstByRingIdAndStatusOrderByScheduledOrderAsc(Long ringId, BoutStatus status);
}
