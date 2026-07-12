package com.boxing.bracket.ring.repository;

import com.boxing.bracket.ring.domain.Ring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface RingRepository extends JpaRepository<Ring, Long> {

    List<Ring> findByTournamentIdOrderByIdAsc(Long tournamentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ring from Ring ring where ring.id = :ringId")
    Optional<Ring> findWithLockById(@Param("ringId") Long ringId);
}
