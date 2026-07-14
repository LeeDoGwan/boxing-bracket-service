package com.boxing.bracket.bout.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.dto.BoutDetailResponse;
import com.boxing.bracket.bout.dto.BoutListResponse;
import com.boxing.bracket.bout.exception.BoutNotFoundException;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.scoring.domain.BoutResult;
import com.boxing.bracket.scoring.repository.BoutResultRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class BoutService {

    private final BoutRepository boutRepository;
    private final AthleteRepository athleteRepository;
    private final BoutResultRepository boutResultRepository;

    public BoutService(
            BoutRepository boutRepository,
            AthleteRepository athleteRepository,
            BoutResultRepository boutResultRepository
    ) {
        this.boutRepository = boutRepository;
        this.athleteRepository = athleteRepository;
        this.boutResultRepository = boutResultRepository;
    }

    public List<BoutListResponse> getOfficialBouts(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        List<Bout> officialBouts = boutRepository.findByTournamentIdOrderByScheduledOrderAsc(tournamentId).stream()
                .filter(bout -> !bout.isEventBout())
                .collect(Collectors.toList());
        Map<Long, BoutResult> resultByBoutId = getResultByBoutId(officialBouts);

        return officialBouts.stream()
                .map(bout -> toListResponse(bout, resultByBoutId.get(bout.getId())))
                .collect(Collectors.toList());
    }

    public List<BoutListResponse> searchOfficialBouts(Long tournamentId, String keyword) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        if (keyword == null || keyword.isBlank()) {
            return getOfficialBouts(tournamentId);
        }

        String normalizedKeyword = keyword.trim();
        Integer boutNumber = parseBoutNumber(normalizedKeyword);
        Set<Long> athleteIds = athleteRepository
                .findByNameContainingIgnoreCaseOrAffiliationContainingIgnoreCase(
                        normalizedKeyword,
                        normalizedKeyword
                )
                .stream()
                .map(Athlete::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Bout> matchedBouts = boutRepository.findByTournamentIdOrderByScheduledOrderAsc(tournamentId).stream()
                .filter(bout -> !bout.isEventBout())
                .filter(bout -> matchesKeyword(bout, normalizedKeyword, boutNumber, athleteIds))
                .collect(Collectors.toList());
        Map<Long, BoutResult> resultByBoutId = getResultByBoutId(matchedBouts);

        return matchedBouts.stream()
                .map(bout -> toListResponse(bout, resultByBoutId.get(bout.getId())))
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
                getAthlete(bout.getBlueAthleteId()),
                boutResultRepository.findByBoutId(boutId).orElse(null)
        );
    }

    private BoutListResponse toListResponse(Bout bout, BoutResult boutResult) {
        return BoutListResponse.of(
                bout,
                getAthlete(bout.getRedAthleteId()),
                getAthlete(bout.getBlueAthleteId()),
                boutResult
        );
    }

    private Map<Long, BoutResult> getResultByBoutId(List<Bout> bouts) {
        Set<Long> boutIds = bouts.stream()
                .map(Bout::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (boutIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<BoutResult> boutResults = boutResultRepository.findByBoutIdIn(boutIds);
        if (boutResults == null) {
            return Collections.emptyMap();
        }

        return boutResults.stream()
                .filter(boutResult -> boutResult.getBoutId() != null)
                .collect(Collectors.toMap(BoutResult::getBoutId, Function.identity(), (first, second) -> first));
    }

    private boolean matchesKeyword(Bout bout, String keyword, Integer boutNumber, Set<Long> athleteIds) {
        return containsIgnoreCase(bout.getMatchType(), keyword)
                || Objects.equals(bout.getBoutNumber(), boutNumber)
                || athleteIds.contains(bout.getRedAthleteId())
                || athleteIds.contains(bout.getBlueAthleteId());
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword.toLowerCase());
    }

    private Integer parseBoutNumber(String keyword) {
        try {
            return Integer.valueOf(keyword);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Athlete getAthlete(Long athleteId) {
        return athleteRepository.findById(athleteId)
                .orElseThrow(() -> new IllegalStateException("Athlete not found"));
    }
}
