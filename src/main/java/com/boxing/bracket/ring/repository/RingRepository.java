package com.boxing.bracket.ring.repository;

import com.boxing.bracket.ring.domain.Ring;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RingRepository extends JpaRepository<Ring, Long> {

    List<Ring> findByTournamentIdOrderByIdAsc(Long tournamentId);
}
