package com.boxing.bracket.athlete.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.dto.AthleteRequest;
import com.boxing.bracket.athlete.dto.AthleteResponse;
import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.common.exception.WorkflowConflictException;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional
public class AdminAthleteService {

    private final AthleteRepository athleteRepository;
    private final BoutRepository boutRepository;

    public AdminAthleteService(AthleteRepository athleteRepository, BoutRepository boutRepository) {
        this.athleteRepository = athleteRepository;
        this.boutRepository = boutRepository;
    }

    @Transactional(readOnly = true)
    public List<AthleteResponse> getAthletes(Long tournamentId, String keyword) {
        validateTournamentId(tournamentId);
        List<Athlete> athletes = keyword == null || keyword.isBlank()
                ? athleteRepository.findByTournamentIdOrderByIdAsc(tournamentId)
                : athleteRepository.searchByTournamentId(tournamentId, keyword.trim());
        return toResponses(athletes);
    }

    @Transactional(readOnly = true)
    public AthleteResponse getAthlete(Long tournamentId, Long athleteId) {
        validateTournamentId(tournamentId);
        validateAthleteId(athleteId);
        return athleteRepository.findByIdAndTournamentId(athleteId, tournamentId)
                .map(AthleteResponse::from)
                .orElseThrow(AthleteNotFoundException::new);
    }

    public AthleteResponse createAthlete(AthleteRequest request) {
        validateRequest(request);
        Athlete athlete = Athlete.builder()
                .name(request.getName())
                .affiliation(request.getAffiliation())
                .build();
        athlete.assignTournament(request.getTournamentId());

        return AthleteResponse.from(athleteRepository.save(athlete));
    }

    public AthleteResponse updateAthlete(Long tournamentId, Long athleteId, AthleteRequest request) {
        validateTournamentId(tournamentId);
        validateAthleteId(athleteId);
        validateRequest(request);
        validateTournamentMatch(tournamentId, request);

        Athlete athlete = athleteRepository.findByIdAndTournamentId(athleteId, tournamentId)
                .orElseThrow(AthleteNotFoundException::new);
        athlete.update(request.getName(), request.getAffiliation());
        return AthleteResponse.from(athleteRepository.save(athlete));
    }

    public void deleteAthlete(Long tournamentId, Long athleteId) {
        validateTournamentId(tournamentId);
        validateAthleteId(athleteId);
        if (!athleteRepository.existsByIdAndTournamentId(athleteId, tournamentId)) {
            throw new AthleteNotFoundException();
        }
        if (boutRepository.existsByRedAthleteIdOrBlueAthleteId(athleteId, athleteId)) {
            throw new WorkflowConflictException("ATHLETE_DELETE_NOT_ALLOWED");
        }

        athleteRepository.deleteById(athleteId);
    }

    private void validateRequest(AthleteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("athlete request is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        validateTournamentId(request.getTournamentId());
    }

    private void validateAthleteId(Long athleteId) {
        if (athleteId == null) {
            throw new IllegalArgumentException("athleteId is required");
        }
    }

    private void validateTournamentId(Long tournamentId) {
        if (tournamentId == null || tournamentId <= 0) {
            throw new IllegalArgumentException("tournamentId is required");
        }
    }

    private void validateTournamentMatch(Long tournamentId, AthleteRequest request) {
        if (!tournamentId.equals(request.getTournamentId())) {
            throw new IllegalArgumentException("tournamentId does not match request");
        }
    }

    private List<AthleteResponse> toResponses(List<Athlete> athletes) {
        return athletes.stream()
                .map(AthleteResponse::from)
                .collect(Collectors.toList());
    }
}
