package com.boxing.bracket.tournament.repository;

import com.boxing.bracket.tournament.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {
}
