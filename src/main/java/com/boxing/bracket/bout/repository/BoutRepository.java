package com.boxing.bracket.bout.repository;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface BoutRepository extends JpaRepository<Bout, Long> {

    List<Bout> findByTournamentIdOrderByScheduledOrderAsc(Long tournamentId);

    List<Bout> findByRingIdOrderByScheduledOrderAsc(Long ringId);

    Optional<Bout> findFirstByRingIdAndStatusOrderByScheduledOrderAsc(Long ringId, BoutStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select bout from Bout bout where bout.id = :boutId")
    Optional<Bout> findWithLockById(@Param("boutId") Long boutId);
}
