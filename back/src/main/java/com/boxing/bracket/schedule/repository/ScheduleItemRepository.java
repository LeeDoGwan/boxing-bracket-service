package com.boxing.bracket.schedule.repository;

import com.boxing.bracket.schedule.domain.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    boolean existsByTournamentId(Long tournamentId);

    List<ScheduleItem> findByTournamentIdOrderByStartTimeAscIdAsc(Long tournamentId);

    boolean existsByRingId(Long ringId);

    boolean existsByRelatedBoutId(Long relatedBoutId);
}
