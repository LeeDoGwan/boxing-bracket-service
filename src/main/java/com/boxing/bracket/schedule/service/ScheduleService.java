package com.boxing.bracket.schedule.service;

import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.boxing.bracket.schedule.repository.ScheduleItemRepository;
import com.boxing.bracket.tournament.exception.TournamentNotFoundException;
import com.boxing.bracket.tournament.repository.TournamentRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final TournamentRepository tournamentRepository;

    public ScheduleService(
            ScheduleItemRepository scheduleItemRepository,
            TournamentRepository tournamentRepository
    ) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.tournamentRepository = tournamentRepository;
    }

    public List<ScheduleResponse> getSchedules(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }

        return scheduleItemRepository.findByTournamentIdOrderByStartTimeAscIdAsc(tournamentId).stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }
}
