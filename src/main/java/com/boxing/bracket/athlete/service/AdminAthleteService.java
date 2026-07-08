package com.boxing.bracket.athlete.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.dto.AthleteRequest;
import com.boxing.bracket.athlete.dto.AthleteResponse;
import com.boxing.bracket.athlete.exception.AthleteNotFoundException;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Lazy
@Transactional
public class AdminAthleteService {

    private final AthleteRepository athleteRepository;

    public AdminAthleteService(AthleteRepository athleteRepository) {
        this.athleteRepository = athleteRepository;
    }

    public AthleteResponse createAthlete(AthleteRequest request) {
        validateRequest(request);
        Athlete athlete = Athlete.builder()
                .name(request.getName())
                .affiliation(request.getAffiliation())
                .build();

        return AthleteResponse.from(athleteRepository.save(athlete));
    }

    public AthleteResponse updateAthlete(Long athleteId, AthleteRequest request) {
        validateAthleteId(athleteId);
        validateRequest(request);

        Athlete athlete = athleteRepository.findById(athleteId)
                .orElseThrow(AthleteNotFoundException::new);
        athlete.update(request.getName(), request.getAffiliation());

        return AthleteResponse.from(athleteRepository.save(athlete));
    }

    public void deleteAthlete(Long athleteId) {
        validateAthleteId(athleteId);
        if (!athleteRepository.existsById(athleteId)) {
            throw new AthleteNotFoundException();
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
    }

    private void validateAthleteId(Long athleteId) {
        if (athleteId == null) {
            throw new IllegalArgumentException("athleteId is required");
        }
    }
}
