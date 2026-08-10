package com.boxing.bracket.tournament.repository;

import com.boxing.bracket.tournament.domain.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tournament from Tournament tournament where tournament.id = :tournamentId")
    Optional<Tournament> findWithLockById(@Param("tournamentId") Long tournamentId);
}
