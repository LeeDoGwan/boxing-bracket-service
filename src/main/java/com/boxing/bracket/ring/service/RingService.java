package com.boxing.bracket.ring.service;

import com.boxing.bracket.athlete.domain.Athlete;
import com.boxing.bracket.athlete.repository.AthleteRepository;
import com.boxing.bracket.bout.domain.Bout;
import com.boxing.bracket.bout.domain.BoutStatus;
import com.boxing.bracket.bout.repository.BoutRepository;
import com.boxing.bracket.ring.domain.Ring;
import com.boxing.bracket.ring.dto.RingBoutSummaryResponse;
import com.boxing.bracket.ring.dto.RingStatusResponse;
import com.boxing.bracket.ring.repository.RingRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Lazy
@Transactional(readOnly = true)
public class RingService {

    private final RingRepository ringRepository;
    private final BoutRepository boutRepository;
    private final AthleteRepository athleteRepository;

    public RingService(
            RingRepository ringRepository,
            BoutRepository boutRepository,
            AthleteRepository athleteRepository
    ) {
        this.ringRepository = ringRepository;
        this.boutRepository = boutRepository;
        this.athleteRepository = athleteRepository;
    }

    public List<RingStatusResponse> getRingStatuses(Long tournamentId) {
        if (tournamentId == null) {
            throw new IllegalArgumentException("tournamentId is required");
        }

        return ringRepository.findByTournamentIdOrderByIdAsc(tournamentId).stream()
                .map(this::toRingStatusResponse)
                .collect(Collectors.toList());
    }

    private RingStatusResponse toRingStatusResponse(Ring ring) {
        List<Bout> officialBouts = boutRepository.findByRingIdOrderByScheduledOrderAsc(ring.getId()).stream()
                .filter(bout -> !bout.isEventBout())
                .collect(Collectors.toList());

        Optional<Bout> currentBout = findCurrentBout(ring, officialBouts);
        Optional<Bout> nextBout = findNextBout(currentBout, officialBouts);

        return RingStatusResponse.of(
                ring,
                currentBout.map(this::toBoutSummaryResponse).orElse(null),
                nextBout.map(this::toBoutSummaryResponse).orElse(null)
        );
    }

    private Optional<Bout> findCurrentBout(Ring ring, List<Bout> bouts) {
        if (ring.getCurrentBoutId() != null) {
            Optional<Bout> currentByRing = bouts.stream()
                    .filter(bout -> ring.getCurrentBoutId().equals(bout.getId()))
                    .findFirst();
            if (currentByRing.isPresent()) {
                return currentByRing;
            }
        }

        return bouts.stream()
                .filter(bout -> bout.getStatus() == BoutStatus.IN_PROGRESS)
                .findFirst();
    }

    private Optional<Bout> findNextBout(Optional<Bout> currentBout, List<Bout> bouts) {
        if (currentBout.isPresent()) {
            Integer currentScheduledOrder = currentBout.get().getScheduledOrder();
            if (currentScheduledOrder == null) {
                return Optional.empty();
            }

            return bouts.stream()
                    .filter(bout -> bout.getScheduledOrder() != null)
                    .filter(bout -> bout.getScheduledOrder() > currentScheduledOrder)
                    .min(Comparator.comparing(Bout::getScheduledOrder));
        }

        return bouts.stream()
                .filter(this::isPreviewStatus)
                .filter(bout -> bout.getScheduledOrder() != null)
                .min(Comparator.comparing(Bout::getScheduledOrder));
    }

    private boolean isPreviewStatus(Bout bout) {
        return bout.getStatus() == BoutStatus.SCHEDULED || bout.getStatus() == BoutStatus.READY;
    }

    private RingBoutSummaryResponse toBoutSummaryResponse(Bout bout) {
        return RingBoutSummaryResponse.of(
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
