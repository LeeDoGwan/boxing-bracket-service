package com.boxing.bracket.bout.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class BoutService {

    private final BoutRepository boutRepository;
    private final AthleteRepository athleteRepository;

    public BoutService(BoutRepository boutRepository, AthleteRepository athleteRepository) {
        this.boutRepository = boutRepository;
        this.athleteRepository = athleteRepository;
    }

    public List<BoutListResponse> getOfficialBouts(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        return boutRepository.findByTournamentIdOrderByScheduledOrderAsc(tournamentId).stream()
                .filter(bout -> !bout.isEventBout())
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    public BoutDetailResponse getBoutDetail(Long boutId) {
        if (boutId == null) {
            throw new IllegalArgumentException("boutId is required");
        }

        Bout bout = boutRepository.findById(boutId)
                .orElseThrow(BoutNotFoundException::new);

        return BoutDetailResponse.of(
                bout,
                getAthlete(bout.getRedAthleteId()),
                getAthlete(bout.getBlueAthleteId())
        );
    }

    private BoutListResponse toListResponse(Bout bout) {
        return BoutListResponse.of(
                bout,
                getAthlete(bout.getRedAthleteId()),
                getAthlete(bout.getBlueAthleteId())
        );
    }

    private Athlete getAthlete(Long athleteId) {
        return athleteRepository.findById(athleteId)
                .orElseThrow(() -> new IllegalStateException("Athlete not found"));
    }
}
