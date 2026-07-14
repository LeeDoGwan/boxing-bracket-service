package com.boxing.bracket.schedule.admin.service;

import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.exception.RingNotFoundException;
import com.boxing.bracket.ring.repository.RingRepository;
import com.boxing.bracket.schedule.admin.dto.ScheduleRequest;
import com.boxing.bracket.schedule.domain.ScheduleItem;
import com.boxing.bracket.schedule.dto.ScheduleResponse;
import com.boxing.bracket.schedule.exception.ScheduleItemNotFoundException;
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
@Transactional
public class AdminScheduleService {

    private final ScheduleItemRepository scheduleItemRepository;
    private final TournamentRepository tournamentRepository;
    private final RingRepository ringRepository;
    private final BoutRepository boutRepository;

    public AdminScheduleService(
            ScheduleItemRepository scheduleItemRepository,
            TournamentRepository tournamentRepository,
            RingRepository ringRepository,
            BoutRepository boutRepository
    ) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.tournamentRepository = tournamentRepository;
        this.ringRepository = ringRepository;
        this.boutRepository = boutRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getSchedules(Long tournamentId) {
        validateTournamentId(tournamentId);
        ensureTournamentExists(tournamentId);
        return scheduleItemRepository.findByTournamentIdOrderByStartTimeAscIdAsc(tournamentId).stream()
                .map(ScheduleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getSchedule(Long scheduleId) {
        validateScheduleId(scheduleId);
        return scheduleItemRepository.findById(scheduleId)
                .map(ScheduleResponse::from)
                .orElseThrow(ScheduleItemNotFoundException::new);
    }

    public ScheduleResponse createSchedule(ScheduleRequest request) {
        validateRequest(request);
        ScheduleItem item = ScheduleItem.builder()
                .tournamentId(request.getTournamentId())
                .ringId(request.getRingId())
                .type(request.getType())
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .relatedBoutId(request.getRelatedBoutId())
                .status(request.getStatus())
                .build();
        return ScheduleResponse.from(scheduleItemRepository.save(item));
    }

    public ScheduleResponse updateSchedule(Long scheduleId, ScheduleRequest request) {
        validateScheduleId(scheduleId);
        validateRequest(request);
        ScheduleItem item = scheduleItemRepository.findById(scheduleId)
                .orElseThrow(ScheduleItemNotFoundException::new);
        item.updateInfo(
                request.getTournamentId(),
                request.getRingId(),
                request.getType(),
                request.getTitle(),
                request.getStartTime(),
                request.getEndTime(),
                request.getRelatedBoutId(),
                request.getStatus()
        );
        return ScheduleResponse.from(scheduleItemRepository.save(item));
    }

    public void deleteSchedule(Long scheduleId) {
        validateScheduleId(scheduleId);
        if (!scheduleItemRepository.existsById(scheduleId)) {
            throw new ScheduleItemNotFoundException();
        }
        scheduleItemRepository.deleteById(scheduleId);
    }

    private void validateRequest(ScheduleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("schedule request is required");
        }
        validateTournamentId(request.getTournamentId());
        ensureTournamentExists(request.getTournamentId());
        validateRing(request.getTournamentId(), request.getRingId());
        validateBout(request.getTournamentId(), request.getRelatedBoutId());
    }

    private void validateRing(Long tournamentId, Long ringId) {
        if (ringId == null) {
            return;
        }
        Ring ring = ringRepository.findById(ringId).orElseThrow(RingNotFoundException::new);
        if (!tournamentId.equals(ring.getTournamentId())) {
            throw new IllegalArgumentException("ring does not belong to tournament");
        }
    }

    private void validateBout(Long tournamentId, Long boutId) {
        if (boutId == null) {
            return;
        }
        Bout bout = boutRepository.findById(boutId).orElseThrow(BoutNotFoundException::new);
        if (!tournamentId.equals(bout.getTournamentId())) {
            throw new IllegalArgumentException("bout does not belong to tournament");
        }
    }

    private void ensureTournamentExists(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw new TournamentNotFoundException();
        }
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }

    private void validateScheduleId(Long scheduleId) {
        if (scheduleId == null) {
            throw new IllegalArgumentException("scheduleId is required");
        }
    }
}
