package com.boxing.bracket.schedule.repository;

import com.boxing.bracket.schedule.domain.ScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleItemRepository extends JpaRepository<ScheduleItem, Long> {

    List<ScheduleItem> findByTournamentIdOrderByStartTimeAscIdAsc(Long tournamentId);
}
